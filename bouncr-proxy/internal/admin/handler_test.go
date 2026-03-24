package admin

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/kawasima/bouncr/bouncr-proxy/internal/auth"
)

const testSigningKey = "test-admin-secret"

func newTestHandler() *Handler {
	return NewHandler(nil, testSigningKey)
}

func TestHealthCheck_Returns200(t *testing.T) {
	handler := newTestHandler()
	mux := handler.ServeMux()

	req := httptest.NewRequest(http.MethodGet, "/_healthcheck", nil)
	rec := httptest.NewRecorder()
	mux.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Errorf("expected status 200, got %d", rec.Code)
	}
	if rec.Body.String() != "OK" {
		t.Errorf("expected body 'OK', got '%s'", rec.Body.String())
	}
}

func TestRefresh_WithoutSignature_Returns401(t *testing.T) {
	handler := newTestHandler()
	mux := handler.ServeMux()

	req := httptest.NewRequest(http.MethodPost, "/_refresh", nil)
	rec := httptest.NewRecorder()
	mux.ServeHTTP(rec, req)

	if rec.Code != http.StatusUnauthorized {
		t.Errorf("expected status 401, got %d", rec.Code)
	}
}

func TestClusters_WithoutSignature_Returns401(t *testing.T) {
	handler := newTestHandler()
	mux := handler.ServeMux()

	req := httptest.NewRequest(http.MethodGet, "/_clusters", nil)
	rec := httptest.NewRecorder()
	mux.ServeHTTP(rec, req)

	if rec.Code != http.StatusUnauthorized {
		t.Errorf("expected status 401, got %d", rec.Code)
	}
}

func TestRefresh_WithInvalidSignature_Returns401(t *testing.T) {
	handler := newTestHandler()
	mux := handler.ServeMux()

	req := httptest.NewRequest(http.MethodPost, "/_refresh", nil)
	req.Header.Set(auth.SignatureHeader, "t=1000000000,v1=deadbeef")
	rec := httptest.NewRecorder()
	mux.ServeHTTP(rec, req)

	if rec.Code != http.StatusUnauthorized {
		t.Errorf("expected status 401, got %d", rec.Code)
	}
}

func TestClusters_WithInvalidSignature_Returns401(t *testing.T) {
	handler := newTestHandler()
	mux := handler.ServeMux()

	req := httptest.NewRequest(http.MethodGet, "/_clusters", nil)
	req.Header.Set(auth.SignatureHeader, "garbage-value")
	rec := httptest.NewRecorder()
	mux.ServeHTTP(rec, req)

	if rec.Code != http.StatusUnauthorized {
		t.Errorf("expected status 401, got %d", rec.Code)
	}
}

func TestHealthCheck_NoSignatureRequired(t *testing.T) {
	// Verify healthcheck works even with no handler configuration issues
	handler := NewHandler(nil, "")
	mux := handler.ServeMux()

	req := httptest.NewRequest(http.MethodGet, "/_healthcheck", nil)
	rec := httptest.NewRecorder()
	mux.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Errorf("expected status 200 even with empty signing key, got %d", rec.Code)
	}
}
