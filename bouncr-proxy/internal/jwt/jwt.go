package jwt

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
)

// Sign creates an HS256 JWT from the given claims and secret.
// Uses base64.RawURLEncoding (no padding) for compatibility with
// Java's Base64.getUrlEncoder().withoutPadding().
func Sign(claims map[string]interface{}, secret []byte) (string, error) {
	header := map[string]string{"alg": "HS256", "typ": "JWT"}
	headerJSON, err := json.Marshal(header)
	if err != nil {
		return "", err
	}
	encodedHeader := base64.RawURLEncoding.EncodeToString(headerJSON)

	claimsJSON, err := json.Marshal(claims)
	if err != nil {
		return "", err
	}
	encodedPayload := base64.RawURLEncoding.EncodeToString(claimsJSON)

	signingInput := encodedHeader + "." + encodedPayload

	mac := hmac.New(sha256.New, secret)
	mac.Write([]byte(signingInput))
	signature := base64.RawURLEncoding.EncodeToString(mac.Sum(nil))

	return signingInput + "." + signature, nil
}
