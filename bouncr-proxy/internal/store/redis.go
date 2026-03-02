package store

import (
	"context"
	"time"

	"github.com/redis/go-redis/v9"
	"github.com/vmihailenco/msgpack/v5"
)

type RedisStore struct {
	client    *redis.Client
	keyPrefix string
}

func NewRedisStore(redisURL, keyPrefix string) (*RedisStore, error) {
	opts, err := redis.ParseURL(redisURL)
	if err != nil {
		return nil, err
	}
	client := redis.NewClient(opts)

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	if err := client.Ping(ctx).Err(); err != nil {
		return nil, err
	}

	return &RedisStore{client: client, keyPrefix: keyPrefix}, nil
}

func (s *RedisStore) Read(ctx context.Context, token string) (map[string]interface{}, error) {
	val, err := s.client.Get(ctx, s.keyPrefix+token).Bytes()
	if err == redis.Nil {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	var result map[string]interface{}
	if err := msgpack.Unmarshal(val, &result); err != nil {
		return nil, err
	}
	return result, nil
}

func (s *RedisStore) Close() error {
	return s.client.Close()
}
