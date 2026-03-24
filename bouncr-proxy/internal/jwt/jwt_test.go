package jwt

import (
	"encoding/base64"
	"encoding/json"
	"strings"
	"testing"
	"time"
)

func TestSign_BasicStructure(t *testing.T) {
	claims := map[string]interface{}{"sub": "user1"}
	token, err := Sign(claims, []byte("secret"), 3600)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	parts := strings.Split(token, ".")
	if len(parts) != 3 {
		t.Fatalf("expected 3 dot-separated parts, got %d", len(parts))
	}
	for i, part := range parts {
		if part == "" {
			t.Errorf("part %d is empty", i)
		}
	}
}

func TestSign_HeaderIsHS256(t *testing.T) {
	claims := map[string]interface{}{"sub": "user1"}
	token, err := Sign(claims, []byte("secret"), 3600)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	parts := strings.Split(token, ".")
	headerJSON, err := base64.RawURLEncoding.DecodeString(parts[0])
	if err != nil {
		t.Fatalf("failed to decode header: %v", err)
	}
	var header map[string]string
	if err := json.Unmarshal(headerJSON, &header); err != nil {
		t.Fatalf("failed to unmarshal header: %v", err)
	}
	if header["alg"] != "HS256" {
		t.Errorf("expected alg=HS256, got %s", header["alg"])
	}
	if header["typ"] != "JWT" {
		t.Errorf("expected typ=JWT, got %s", header["typ"])
	}
}

func TestSign_IatAndExpAreSet(t *testing.T) {
	before := time.Now().Unix()
	claims := map[string]interface{}{"sub": "user1"}
	token, err := Sign(claims, []byte("secret"), 3600)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	after := time.Now().Unix()

	decoded := decodeClaims(t, token)

	iat, ok := decoded["iat"].(float64)
	if !ok {
		t.Fatal("iat claim missing or not a number")
	}
	if int64(iat) < before || int64(iat) > after {
		t.Errorf("iat %v not in range [%d, %d]", iat, before, after)
	}

	exp, ok := decoded["exp"].(float64)
	if !ok {
		t.Fatal("exp claim missing or not a number")
	}
	expectedExp := int64(iat) + 3600
	if int64(exp) != expectedExp {
		t.Errorf("expected exp=%d, got %v", expectedExp, exp)
	}
}

func TestSign_CustomClaimsPreserved(t *testing.T) {
	claims := map[string]interface{}{
		"sub":         "user1",
		"permissions": []string{"read", "write"},
		"realm_id":    42,
	}
	token, err := Sign(claims, []byte("secret"), 3600)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	decoded := decodeClaims(t, token)

	if decoded["sub"] != "user1" {
		t.Errorf("expected sub=user1, got %v", decoded["sub"])
	}
	if decoded["realm_id"] != float64(42) {
		t.Errorf("expected realm_id=42, got %v", decoded["realm_id"])
	}
	perms, ok := decoded["permissions"].([]interface{})
	if !ok {
		t.Fatal("permissions claim missing or wrong type")
	}
	if len(perms) != 2 {
		t.Errorf("expected 2 permissions, got %d", len(perms))
	}
}

func TestSign_CustomExpOverridesDefault(t *testing.T) {
	customExp := int64(9999999999)
	claims := map[string]interface{}{
		"sub": "user1",
		"exp": customExp,
	}
	token, err := Sign(claims, []byte("secret"), 3600)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	decoded := decodeClaims(t, token)
	exp, ok := decoded["exp"].(float64)
	if !ok {
		t.Fatal("exp claim missing or not a number")
	}
	if int64(exp) != customExp {
		t.Errorf("expected custom exp=%d, got %v", customExp, exp)
	}
}

func TestSign_CustomIatOverridesDefault(t *testing.T) {
	customIat := int64(1000000000)
	claims := map[string]interface{}{
		"sub": "user1",
		"iat": customIat,
	}
	token, err := Sign(claims, []byte("secret"), 3600)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	decoded := decodeClaims(t, token)
	iat, ok := decoded["iat"].(float64)
	if !ok {
		t.Fatal("iat claim missing or not a number")
	}
	if int64(iat) != customIat {
		t.Errorf("expected custom iat=%d, got %v", customIat, iat)
	}
}

func TestSign_DifferentSecretsProduceDifferentSignatures(t *testing.T) {
	claims1 := map[string]interface{}{"sub": "user1", "iat": int64(1000), "exp": int64(2000)}
	claims2 := map[string]interface{}{"sub": "user1", "iat": int64(1000), "exp": int64(2000)}
	token1, err := Sign(claims1, []byte("secret-a"), 3600)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	token2, err := Sign(claims2, []byte("secret-b"), 3600)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	sig1 := strings.Split(token1, ".")[2]
	sig2 := strings.Split(token2, ".")[2]
	if sig1 == sig2 {
		t.Error("expected different signatures for different secrets")
	}
}

func TestSign_ExpirationSecondsUsedCorrectly(t *testing.T) {
	before := time.Now().Unix()
	claims := map[string]interface{}{"sub": "user1"}
	token, err := Sign(claims, []byte("secret"), 7200)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	after := time.Now().Unix()

	decoded := decodeClaims(t, token)
	iat := int64(decoded["iat"].(float64))
	exp := int64(decoded["exp"].(float64))

	if iat < before || iat > after {
		t.Errorf("iat %d not in range [%d, %d]", iat, before, after)
	}
	if exp != iat+7200 {
		t.Errorf("expected exp = iat+7200 = %d, got %d", iat+7200, exp)
	}
}

func TestSign_UsesRawURLEncoding(t *testing.T) {
	claims := map[string]interface{}{"sub": "user1"}
	token, err := Sign(claims, []byte("secret"), 3600)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if strings.Contains(token, "=") {
		t.Error("token contains padding characters; expected RawURLEncoding (no padding)")
	}
	if strings.Contains(token, "+") || strings.Contains(token, "/") {
		t.Error("token contains standard base64 characters; expected URL-safe encoding")
	}
}

// decodeClaims is a test helper that extracts the claims from a JWT token.
func decodeClaims(t *testing.T, token string) map[string]interface{} {
	t.Helper()
	parts := strings.Split(token, ".")
	if len(parts) != 3 {
		t.Fatalf("invalid token structure: %d parts", len(parts))
	}
	payloadJSON, err := base64.RawURLEncoding.DecodeString(parts[1])
	if err != nil {
		t.Fatalf("failed to decode payload: %v", err)
	}
	var claims map[string]interface{}
	if err := json.Unmarshal(payloadJSON, &claims); err != nil {
		t.Fatalf("failed to unmarshal claims: %v", err)
	}
	return claims
}
