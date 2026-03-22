package auth

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"strconv"
	"strings"
	"time"
)

const SignatureHeader = "X-Bouncr-Signature"
const toleranceSeconds = 30

// SignRequest computes an HMAC-SHA256 signature over "{timestamp}.{payload}"
// and returns a Stripe-style header value: "t={timestamp},v1={hex-hmac}".
func SignRequest(key []byte, payload string) string {
	t := time.Now().Unix()
	signed := fmt.Sprintf("%d.%s", t, payload)
	mac := hmac.New(sha256.New, key)
	mac.Write([]byte(signed))
	sig := hex.EncodeToString(mac.Sum(nil))
	return fmt.Sprintf("t=%d,v1=%s", t, sig)
}

// VerifySignature verifies an HMAC-SHA256 signature header value against
// the given payload and key. Returns true if the signature is valid and
// the timestamp is within ±30 seconds.
func VerifySignature(key []byte, header string, payload string) bool {
	if header == "" {
		return false
	}
	var timestamp, signature string
	for _, part := range strings.Split(header, ",") {
		if strings.HasPrefix(part, "t=") {
			timestamp = part[2:]
		} else if strings.HasPrefix(part, "v1=") {
			signature = part[3:]
		}
	}
	if timestamp == "" || signature == "" {
		return false
	}

	t, err := strconv.ParseInt(timestamp, 10, 64)
	if err != nil {
		return false
	}
	now := time.Now().Unix()
	diff := now - t
	if diff < -toleranceSeconds || diff > toleranceSeconds {
		return false
	}

	signed := fmt.Sprintf("%s.%s", timestamp, payload)
	mac := hmac.New(sha256.New, key)
	mac.Write([]byte(signed))
	expected := mac.Sum(nil)

	provided, err := hex.DecodeString(signature)
	if err != nil {
		return false
	}
	return hmac.Equal(expected, provided)
}
