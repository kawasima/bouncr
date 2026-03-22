package auth

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/kawasima/bouncr/bouncr-proxy/internal/jwt"
	"github.com/kawasima/bouncr/bouncr-proxy/internal/realm"
	"github.com/kawasima/bouncr/bouncr-proxy/internal/store"
)

type Authenticator struct {
	store             *store.RedisStore
	realmCache        *realm.Cache
	jwtSecret         []byte
	cookieName        string
	backendHeaderName string
	apiServerURL      string
}

func NewAuthenticator(
	store *store.RedisStore,
	realmCache *realm.Cache,
	jwtSecret string,
	cookieName string,
	backendHeaderName string,
	apiServerURL string,
) *Authenticator {
	return &Authenticator{
		store:             store,
		realmCache:        realmCache,
		jwtSecret:         []byte(jwtSecret),
		cookieName:        cookieName,
		backendHeaderName: backendHeaderName,
		apiServerURL:      apiServerURL,
	}
}

// AuthResult holds the result of authentication and routing.
type AuthResult struct {
	HeaderName  string
	HeaderValue string
	// Routing information from the matched realm's application
	ClusterName string // Envoy cluster name for the backend
	RewritePath string // Rewritten path for the backend
}

// Authenticate extracts a token, looks it up in Redis, and generates a JWT.
// It also resolves routing information (cluster + path rewrite) from the matched realm.
// Returns nil if no realm matches the path.
func (a *Authenticator) Authenticate(ctx context.Context, headers map[string]string, path string) (*AuthResult, error) {
	// Strip query string for realm matching; preserve the original path (with query)
	// so the rewritten :path forwarded to the backend retains query parameters.
	matchPath := path
	if i := strings.IndexByte(path, '?'); i >= 0 {
		matchPath = path[:i]
	}

	// Find matching realm first (needed for both auth and routing)
	matchedRealm := a.realmCache.Match(matchPath)
	if matchedRealm == nil {
		return nil, nil
	}

	app := matchedRealm.Application

	// Calculate rewritten path: strip virtualPath, prepend backend path.
	// Use original path so query parameters are preserved in the rewrite.
	rewritePath := rewriteRequestPath(path, app.VirtualPath, app.BackendPath())

	result := &AuthResult{
		ClusterName: app.ClusterName(),
		RewritePath: rewritePath,
	}

	// Extract and validate token
	token := ExtractToken(headers, a.cookieName)
	if token == "" {
		// No token: still route, but no credential header
		return result, nil
	}

	profileMap, err := a.store.Read(ctx, token)
	if err != nil {
		return nil, fmt.Errorf("redis read: %w", err)
	}
	if profileMap == nil {
		// Access token cache expired — try refresh via API server
		profileMap, err = a.refreshFromAPIServer(ctx, token)
		if err != nil {
			log.Printf("refresh failed for token %s: %v", truncateToken(token), err)
		}
		if profileMap == nil {
			// Both expired — no credential header
			return result, nil
		}
		log.Printf("refreshed access token for session %s", truncateToken(token))
	}

	// Extract permissionsByRealm and filter to matched realm
	var permissions []interface{}
	if pbr, ok := profileMap["permissionsByRealm"]; ok {
		if pbrMap, ok := pbr.(map[string]interface{}); ok {
			realmKey := strconv.FormatInt(matchedRealm.ID, 10)
			if perms, ok := pbrMap[realmKey]; ok {
				if permList, ok := perms.([]interface{}); ok {
					permissions = permList
				}
			}
		}
		delete(profileMap, "permissionsByRealm")
	}

	if permissions == nil {
		permissions = []interface{}{}
	}

	// Build JWT claims
	claims := make(map[string]interface{})
	for k, v := range profileMap {
		claims[k] = v
	}
	claims["permissions"] = permissions

	jwtToken, err := jwt.Sign(claims, a.jwtSecret)
	if err != nil {
		return nil, fmt.Errorf("jwt sign: %w", err)
	}

	log.Printf("authenticated user %v for realm %d", claims["sub"], matchedRealm.ID)

	result.HeaderName = a.backendHeaderName
	result.HeaderValue = jwtToken
	return result, nil
}

// refreshFromAPIServer calls the API server's /token/refresh endpoint to
// rebuild the access token cache from DB. Returns the fresh profileMap or nil.
func (a *Authenticator) refreshFromAPIServer(ctx context.Context, sessionID string) (map[string]interface{}, error) {
	if a.apiServerURL == "" {
		return nil, fmt.Errorf("API_SERVER_URL not configured")
	}

	body, err := json.Marshal(map[string]string{"session_id": sessionID})
	if err != nil {
		return nil, fmt.Errorf("marshal refresh request: %w", err)
	}
	url := strings.TrimRight(a.apiServerURL, "/") + "/bouncr/api/token/refresh"

	reqCtx, cancel := context.WithTimeout(ctx, 3*time.Second)
	defer cancel()

	req, err := http.NewRequestWithContext(reqCtx, http.MethodPost, url, bytes.NewReader(body))
	if err != nil {
		return nil, fmt.Errorf("create request: %w", err)
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("http call: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusCreated && resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("refresh returned %d", resp.StatusCode)
	}

	var result struct {
		Profile map[string]interface{} `json:"profile"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, fmt.Errorf("decode response: %w", err)
	}

	return result.Profile, nil
}

// truncateToken safely returns the first 8 characters of a token for logging.
func truncateToken(token string) string {
	if len(token) <= 8 {
		return token
	}
	return token[:8]
}

// rewriteRequestPath strips the virtualPath prefix and prepends the backend path.
// This matches MultiAppProxyClient.calculatePathTo() in bouncr-proxy.
func rewriteRequestPath(requestPath, virtualPath, backendPath string) string {
	suffix := strings.TrimPrefix(requestPath, virtualPath)
	return backendPath + suffix
}
