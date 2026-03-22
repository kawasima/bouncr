package auth

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"strings"
	"testing"
)

func TestSignRequest(t *testing.T) {
	key := []byte("test-secret-key")
	payload := "test-session-id"

	result := SignRequest(key, payload)

	// Parse the result
	if !strings.HasPrefix(result, "t=") {
		t.Fatalf("expected result to start with 't=', got: %s", result)
	}
	parts := strings.SplitN(result, ",", 2)
	if len(parts) != 2 {
		t.Fatalf("expected 2 parts separated by comma, got: %s", result)
	}
	if !strings.HasPrefix(parts[0], "t=") {
		t.Fatalf("expected first part to start with 't=', got: %s", parts[0])
	}
	if !strings.HasPrefix(parts[1], "v1=") {
		t.Fatalf("expected second part to start with 'v1=', got: %s", parts[1])
	}

	// Verify the HMAC
	timestamp := strings.TrimPrefix(parts[0], "t=")
	sig := strings.TrimPrefix(parts[1], "v1=")

	signed := fmt.Sprintf("%s.%s", timestamp, payload)
	mac := hmac.New(sha256.New, key)
	mac.Write([]byte(signed))
	expected := hex.EncodeToString(mac.Sum(nil))

	if sig != expected {
		t.Errorf("HMAC mismatch: got %s, expected %s", sig, expected)
	}
}

func TestSignatureHeaderConstant(t *testing.T) {
	if SignatureHeader != "X-Bouncr-Signature" {
		t.Errorf("expected X-Bouncr-Signature, got %s", SignatureHeader)
	}
}

func TestVerifySignature_validSignature(t *testing.T) {
	key := []byte("test-secret-key")
	payload := "test-session-id"

	header := SignRequest(key, payload)
	if !VerifySignature(key, header, payload) {
		t.Error("expected valid signature to verify")
	}
}

func TestVerifySignature_wrongKey(t *testing.T) {
	key := []byte("test-secret-key")
	wrongKey := []byte("wrong-key")
	payload := "test-session-id"

	header := SignRequest(key, payload)
	if VerifySignature(wrongKey, header, payload) {
		t.Error("expected verification to fail with wrong key")
	}
}

func TestVerifySignature_wrongPayload(t *testing.T) {
	key := []byte("test-secret-key")
	payload := "test-session-id"

	header := SignRequest(key, payload)
	if VerifySignature(key, header, "different-payload") {
		t.Error("expected verification to fail with different payload")
	}
}

func TestVerifySignature_emptyHeader(t *testing.T) {
	key := []byte("test-secret-key")
	if VerifySignature(key, "", "payload") {
		t.Error("expected verification to fail with empty header")
	}
}

func TestVerifySignature_expiredTimestamp(t *testing.T) {
	key := []byte("test-secret-key")
	payload := "test-session-id"

	// Manually construct a signature with an old timestamp
	oldTimestamp := "1000000000"
	signed := fmt.Sprintf("%s.%s", oldTimestamp, payload)
	mac := hmac.New(sha256.New, key)
	mac.Write([]byte(signed))
	sig := hex.EncodeToString(mac.Sum(nil))
	header := fmt.Sprintf("t=%s,v1=%s", oldTimestamp, sig)

	if VerifySignature(key, header, payload) {
		t.Error("expected verification to fail with expired timestamp")
	}
}

func TestVerifySignature_malformedHeader(t *testing.T) {
	key := []byte("test-secret-key")
	if VerifySignature(key, "garbage", "payload") {
		t.Error("expected verification to fail with malformed header")
	}
}
