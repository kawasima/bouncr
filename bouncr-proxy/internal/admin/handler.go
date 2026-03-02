package admin

import (
	"encoding/json"
	"log"
	"net/http"

	"github.com/kawasima/bouncr/bouncr-proxy/internal/realm"
)

// Handler provides HTTP endpoints for operational management.
type Handler struct {
	realmCache *realm.Cache
}

func NewHandler(realmCache *realm.Cache) *Handler {
	return &Handler{realmCache: realmCache}
}

// ServeMux returns an http.ServeMux with registered admin endpoints.
func (h *Handler) ServeMux() *http.ServeMux {
	mux := http.NewServeMux()
	mux.HandleFunc("POST /_refresh", h.handleRefresh)
	mux.HandleFunc("GET /_clusters", h.handleClusters)
	mux.HandleFunc("GET /_healthcheck", h.handleHealthCheck)
	return mux
}

// handleRefresh triggers an immediate realm cache refresh.
func (h *Handler) handleRefresh(w http.ResponseWriter, r *http.Request) {
	if err := h.realmCache.Refresh(); err != nil {
		log.Printf("admin: refresh error: %v", err)
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	log.Printf("admin: realm cache refreshed on demand")
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
			log.Printf("admin: failed to parse pass_to for app %d: %v", app.ID, err)
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
