package main

import (
	"fmt"
	"log"
	"log/slog"
	"net"
	"net/http"
	"os"
	"strings"

	extprocpb "github.com/envoyproxy/go-control-plane/envoy/service/ext_proc/v3"
	"github.com/kawasima/bouncr/bouncr-proxy/internal/admin"
	"github.com/kawasima/bouncr/bouncr-proxy/internal/auth"
	"github.com/kawasima/bouncr/bouncr-proxy/internal/config"
	"github.com/kawasima/bouncr/bouncr-proxy/internal/envoyconf"
	"github.com/kawasima/bouncr/bouncr-proxy/internal/extproc"
	"github.com/kawasima/bouncr/bouncr-proxy/internal/realm"
	"github.com/kawasima/bouncr/bouncr-proxy/internal/store"
	"google.golang.org/grpc"
)

func main() {
	initLogger()

	// Check for subcommands
	if len(os.Args) > 1 {
		switch os.Args[1] {
		case "gen-envoy-config":
			runGenEnvoyConfig()
			return
		}
	}

	runServer()
}

// initLogger sets the default slog level from the LOG_LEVEL environment variable.
// Supported values: debug, info, warn, error (default: info).
func initLogger() {
	level := slog.LevelInfo
	switch strings.ToLower(os.Getenv("LOG_LEVEL")) {
	case "debug":
		level = slog.LevelDebug
	case "warn", "warning":
		level = slog.LevelWarn
	case "error":
		level = slog.LevelError
	}
	slog.SetDefault(slog.New(slog.NewTextHandler(os.Stderr, &slog.HandlerOptions{Level: level})))
}

func runServer() {
	cfg := config.Load()

	if cfg.JWTSecret == "" {
		log.Fatal("JWT_SECRET is required")
	}
	if cfg.DBDSN == "" {
		log.Fatal("DB_DSN is required")
	}
	if cfg.InternalSigningKey == "" {
		log.Fatal("INTERNAL_SIGNING_KEY is required")
	}

	// Initialize Redis store
	redisStore, err := store.NewRedisStore(cfg.RedisURL, cfg.RedisKeyPrefix)
	if err != nil {
		log.Fatalf("failed to connect to Redis: %v", err)
	}
	defer redisStore.Close()

	// Initialize realm cache
	realmCache, err := realm.NewCache(cfg.DBDSN)
	if err != nil {
		log.Fatalf("failed to initialize realm cache: %v", err)
	}
	defer realmCache.Close()
	realmCache.StartPeriodicRefresh(cfg.RealmRefreshInterval)

	// Initialize authenticator
	authenticator := auth.NewAuthenticator(
		redisStore,
		realmCache,
		cfg.JWTSecret,
		cfg.JWTExpiration,
		cfg.TokenCookieName,
		cfg.BackendHeaderName,
		cfg.APIServerURL,
		cfg.InternalSigningKey,
	)

	// Start admin HTTP server (for /_refresh, /_clusters, /_healthcheck)
	adminHandler := admin.NewHandler(realmCache, cfg.InternalSigningKey)
	go func() {
		addr := fmt.Sprintf(":%s", cfg.AdminPort)
		slog.Info("admin HTTP server listening", "addr", addr)
		if err := http.ListenAndServe(addr, adminHandler.ServeMux()); err != nil {
			log.Fatalf("admin HTTP server failed: %v", err)
		}
	}()

	// Start gRPC server
	lis, err := net.Listen("tcp", fmt.Sprintf(":%s", cfg.GRPCPort))
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}

	grpcServer := grpc.NewServer()
	extprocpb.RegisterExternalProcessorServer(grpcServer, extproc.NewServer(authenticator))

	slog.Info("bouncr-proxy ext_proc server listening", "port", cfg.GRPCPort)
	if err := grpcServer.Serve(lis); err != nil {
		log.Fatalf("gRPC server failed: %v", err)
	}
}

// runGenEnvoyConfig generates Envoy configuration from the database.
func runGenEnvoyConfig() {
	cfg := config.Load()

	if cfg.DBDSN == "" {
		log.Fatal("DB_DSN is required")
	}

	realmCache, err := realm.NewCache(cfg.DBDSN)
	if err != nil {
		log.Fatalf("failed to initialize realm cache: %v", err)
	}
	defer realmCache.Close()

	apps := realmCache.Applications()
	if len(apps) == 0 {
		log.Fatal("no applications found in database")
	}

	grpcPort := 50051
	if _, err := fmt.Sscanf(cfg.GRPCPort, "%d", &grpcPort); err != nil {
		slog.Warn("invalid GRPC_PORT, using default", "value", cfg.GRPCPort, "default", grpcPort, "error", err)
	}

	listenPort := 3000
	if _, err := fmt.Sscanf(cfg.ListenPort, "%d", &listenPort); err != nil {
		slog.Warn("invalid LISTEN_PORT, using default", "value", cfg.ListenPort, "default", listenPort, "error", err)
	}

	if err := envoyconf.GenerateFullConfig(os.Stdout, apps, grpcPort, listenPort); err != nil {
		log.Fatalf("failed to generate config: %v", err)
	}
}
