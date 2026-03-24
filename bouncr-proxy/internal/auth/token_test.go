package auth

import "testing"

func TestExtractToken_BearerHeader(t *testing.T) {
	headers := map[string]string{
		"authorization": "Bearer my-token-123",
	}
	got := ExtractToken(headers, "bouncr_token")
	if got != "my-token-123" {
		t.Errorf("expected 'my-token-123', got '%s'", got)
	}
}

func TestExtractToken_BearerHeaderCaseInsensitive(t *testing.T) {
	tests := []struct {
		name   string
		header string
	}{
		{"lowercase", "bearer my-token"},
		{"uppercase", "BEARER my-token"},
		{"mixedcase", "BeArEr my-token"},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			headers := map[string]string{"authorization": tt.header}
			got := ExtractToken(headers, "bouncr_token")
			if got != "my-token" {
				t.Errorf("expected 'my-token', got '%s'", got)
			}
		})
	}
}

func TestExtractToken_BearerWithExtraWhitespace(t *testing.T) {
	headers := map[string]string{
		"authorization": "Bearer   my-token  ",
	}
	got := ExtractToken(headers, "bouncr_token")
	if got != "my-token" {
		t.Errorf("expected 'my-token', got '%s'", got)
	}
}

func TestExtractToken_NonBearerAuth(t *testing.T) {
	headers := map[string]string{
		"authorization": "Basic dXNlcjpwYXNz",
	}
	got := ExtractToken(headers, "bouncr_token")
	if got != "" {
		t.Errorf("expected empty string for non-Bearer auth, got '%s'", got)
	}
}

func TestExtractToken_AuthorizationWithoutScheme(t *testing.T) {
	headers := map[string]string{
		"authorization": "just-a-token",
	}
	got := ExtractToken(headers, "bouncr_token")
	if got != "" {
		t.Errorf("expected empty string for auth without scheme, got '%s'", got)
	}
}

func TestExtractToken_CookieFallback(t *testing.T) {
	headers := map[string]string{
		"cookie": "bouncr_token=cookie-token-456; other=xyz",
	}
	got := ExtractToken(headers, "bouncr_token")
	if got != "cookie-token-456" {
		t.Errorf("expected 'cookie-token-456', got '%s'", got)
	}
}

func TestExtractToken_CookieNotFound(t *testing.T) {
	headers := map[string]string{
		"cookie": "other=xyz; session=abc",
	}
	got := ExtractToken(headers, "bouncr_token")
	if got != "" {
		t.Errorf("expected empty string when cookie not found, got '%s'", got)
	}
}

func TestExtractToken_EmptyHeaders(t *testing.T) {
	got := ExtractToken(map[string]string{}, "bouncr_token")
	if got != "" {
		t.Errorf("expected empty string for empty headers, got '%s'", got)
	}
}

func TestExtractToken_NilHeaders(t *testing.T) {
	got := ExtractToken(nil, "bouncr_token")
	if got != "" {
		t.Errorf("expected empty string for nil headers, got '%s'", got)
	}
}

func TestExtractToken_AuthorizationTakesPrecedenceOverCookie(t *testing.T) {
	headers := map[string]string{
		"authorization": "Bearer auth-token",
		"cookie":        "bouncr_token=cookie-token",
	}
	got := ExtractToken(headers, "bouncr_token")
	if got != "auth-token" {
		t.Errorf("expected 'auth-token' (from Authorization), got '%s'", got)
	}
}

func TestExtractToken_EmptyBearerValue(t *testing.T) {
	headers := map[string]string{
		"authorization": "Bearer ",
	}
	got := ExtractToken(headers, "bouncr_token")
	if got != "" {
		t.Errorf("expected empty string for empty bearer value, got '%s'", got)
	}
}
