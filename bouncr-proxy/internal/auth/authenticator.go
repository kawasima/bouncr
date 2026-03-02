package auth

import (
	"context"
	"fmt"
	"log"
	"strconv"
	"strings"

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
}

func NewAuthenticator(
	store *store.RedisStore,
	realmCache *realm.Cache,
	jwtSecret string,
	cookieName string,
	backendHeaderName string,
) *Authenticator {
	return &Authenticator{
		store:             store,
		realmCache:        realmCache,
		jwtSecret:         []byte(jwtSecret),
		cookieName:        cookieName,
		backendHeaderName: backendHeaderName,
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
	// Find matching realm first (needed for both auth and routing)
	matchedRealm := a.realmCache.Match(path)
	if matchedRealm == nil {
		return nil, nil
	}

	app := matchedRealm.Application

	// Calculate rewritten path: strip virtualPath, prepend backend path
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
		// Invalid/expired token: still route, but no credential header
		return result, nil
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

// rewriteRequestPath strips the virtualPath prefix and prepends the backend path.
// This matches MultiAppProxyClient.calculatePathTo() in bouncr-proxy.
func rewriteRequestPath(requestPath, virtualPath, backendPath string) string {
	suffix := strings.TrimPrefix(requestPath, virtualPath)
	return backendPath + suffix
}
