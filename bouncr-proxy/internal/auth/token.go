package auth

import (
	"net/http"
	"strings"
)

// ExtractToken extracts the bearer token from the Authorization header,
// or falls back to the named cookie.
// Replicates MultiAppProxyClient.parseToken() (lines 144-160).
func ExtractToken(headers map[string]string, cookieName string) string {
	if auth, ok := headers["authorization"]; ok {
		parts := strings.SplitN(auth, " ", 2)
		if len(parts) == 2 && strings.EqualFold(parts[0], "bearer") {
			return strings.TrimSpace(parts[1])
		}
	}

	if cookieHeader, ok := headers["cookie"]; ok {
		// Parse using net/http cookie parser
		header := http.Header{}
		header.Add("Cookie", cookieHeader)
		request := http.Request{Header: header}
		for _, c := range request.Cookies() {
			if c.Name == cookieName {
				return c.Value
			}
		}
	}

	return ""
}
