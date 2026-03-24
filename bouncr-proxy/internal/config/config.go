package config

import (
	"log/slog"
	"os"
	"strconv"
	"time"
)

type Config struct {
	GRPCPort             string
	AdminPort            string
	ListenPort           string
	RedisURL             string
	RedisKeyPrefix       string
	DBDSN                string
	JWTSecret            string
	JWTExpiration        int64
	TokenCookieName      string
	BackendHeaderName    string
	RealmRefreshInterval time.Duration
	APIServerURL         string
	InternalSigningKey   string
}

func Load() *Config {
	return &Config{
		GRPCPort:             envOrDefault("GRPC_PORT", "50051"),
		AdminPort:            envOrDefault("ADMIN_PORT", "8081"),
		ListenPort:           envOrDefault("LISTEN_PORT", "3000"),
		RedisURL:             envOrDefault("REDIS_URL", "redis://localhost:6379"),
		RedisKeyPrefix:       envOrDefault("REDIS_KEY_PREFIX", "BOUNCR_TOKEN:"),
		DBDSN:                envOrDefault("DB_DSN", ""),
		JWTSecret:            envOrDefault("JWT_SECRET", ""),
		JWTExpiration:        parseIntOrDefault(envOrDefault("JWT_EXPIRATION", "300"), 300),
		TokenCookieName:      envOrDefault("TOKEN_COOKIE_NAME", "BOUNCR_TOKEN"),
		BackendHeaderName:    envOrDefault("BACKEND_HEADER_NAME", "x-bouncr-credential"),
		RealmRefreshInterval: parseDuration(envOrDefault("REALM_REFRESH_INTERVAL", "30s")),
		APIServerURL:         envOrDefault("API_SERVER_URL", "http://localhost:3005"),
		InternalSigningKey:   envOrDefault("INTERNAL_SIGNING_KEY", ""),
	}
}

func envOrDefault(key, defaultValue string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return defaultValue
}

func parseDuration(s string) time.Duration {
	d, err := time.ParseDuration(s)
	if err != nil {
		slog.Warn("invalid REALM_REFRESH_INTERVAL, using default 30s", "value", s, "error", err)
		return 30 * time.Second
	}
	return d
}

func parseIntOrDefault(s string, defaultValue int64) int64 {
	v, err := strconv.ParseInt(s, 10, 64)
	if err != nil {
		return defaultValue
	}
	return v
}
