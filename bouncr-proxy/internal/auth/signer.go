package auth

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"time"
)

const SignatureHeader = "X-Bouncr-Signature"

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
