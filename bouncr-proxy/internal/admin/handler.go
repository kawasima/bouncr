package admin

import (
	"encoding/json"
	"log/slog"
	"net/http"

	"github.com/kawasima/bouncr/bouncr-proxy/internal/auth"
	"github.com/kawasima/bouncr/bouncr-proxy/internal/realm"
)

// Handler provides HTTP endpoints for operational management.
type Handler struct {
	realmCache         *realm.Cache
	internalSigningKey []byte
}

func NewHandler(realmCache *realm.Cache, internalSigningKey string) *Handler {
	return &Handler{
		realmCache:         realmCache,
		internalSigningKey: []byte(internalSigningKey),
	}
}

// ServeMux returns an http.ServeMux with registered admin endpoints.
func (h *Handler) ServeMux() *http.ServeMux {
	mux := http.NewServeMux()
	mux.HandleFunc("POST /_refresh", h.requireSignature(h.handleRefresh))
	mux.HandleFunc("GET /_clusters", h.requireSignature(h.handleClusters))
	mux.HandleFunc("GET /_healthcheck", h.handleHealthCheck)
	return mux
}

// requireSignature wraps a handler to verify the X-Bouncr-Signature header.
// The signature payload is the request path (e.g. "/_refresh").
func (h *Handler) requireSignature(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		sig := r.Header.Get(auth.SignatureHeader)
		if !auth.VerifySignature(h.internalSigningKey, sig, r.URL.Path) {
			http.Error(w, "Unauthorized", http.StatusUnauthorized)
			return
		}
		next(w, r)
	}
}

// handleRefresh triggers an immediate realm cache refresh.
func (h *Handler) handleRefresh(w http.ResponseWriter, r *http.Request) {
	if err := h.realmCache.Refresh(); err != nil {
		slog.Error("realm cache refresh failed", "error", err)
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	slog.Info("realm cache refreshed on demand")
	w.WriteHeader(http.StatusNoContent)
}

// handleClusters returns Envoy cluster definitions derived from DB applications.
// This can be used to generate or verify the Envoy static cluster config.
func (h *Handler) handleClusters(w http.ResponseWriter, r *http.Request) {
	apps := h.realmCache.Applications()

	type clusterInfo struct {
		ClusterName string `json:"cluster_name"`
		Address     string `json:"address"`
		Port        int    `json:"port"`
		VirtualPath string `json:"virtual_path"`
		PassTo      string `json:"pass_to"`
	}

	var clusters []clusterInfo
	for _, app := range apps {
		host, port, err := app.BackendAddress()
		if err != nil {
			slog.Warn("failed to parse pass_to", "app_id", app.ID, "error", err)
			continue
		}
		clusters = append(clusters, clusterInfo{
			ClusterName: app.ClusterName(),
			Address:     host,
			Port:        port,
			VirtualPath: app.VirtualPath,
			PassTo:      app.PassTo,
		})
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(clusters)
}

// handleHealthCheck returns 200 OK.
func (h *Handler) handleHealthCheck(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusOK)
	w.Write([]byte("OK"))
}
