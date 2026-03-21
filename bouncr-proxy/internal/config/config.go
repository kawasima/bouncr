package config

import (
	"os"
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
	TokenCookieName      string
	BackendHeaderName    string
	RealmRefreshInterval time.Duration
	APIServerURL         string
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
		TokenCookieName:      envOrDefault("TOKEN_COOKIE_NAME", "BOUNCR_TOKEN"),
		BackendHeaderName:    envOrDefault("BACKEND_HEADER_NAME", "x-bouncr-credential"),
		RealmRefreshInterval: parseDuration(envOrDefault("REALM_REFRESH_INTERVAL", "30s")),
		APIServerURL:         envOrDefault("API_SERVER_URL", "http://localhost:3005"),
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
		return 30 * time.Second
	}
	return d
}
