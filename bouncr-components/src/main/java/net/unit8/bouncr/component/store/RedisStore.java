package net.unit8.bouncr.component.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import enkan.middleware.session.KeyValueStore;
import redis.clients.jedis.JedisPooled;

import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.HashMap;

public class RedisStore implements KeyValueStore {
    private final JedisPooled jedis;
    private final ObjectMapper mapper;
    private final String keyPrefix;
    private final long ttlSeconds;

    public RedisStore(JedisPooled jedis, String keyPrefix, long ttlSeconds) {
        this.jedis = jedis;
        this.mapper = new ObjectMapper();
        this.keyPrefix = keyPrefix;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public Serializable read(String key) {
        String json = jedis.get(keyPrefix + key);
        if (json == null) return null;
        try {
            return mapper.readValue(json, HashMap.class);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(new java.io.IOException(e));
        }
    }

    @Override
    public String write(String key, Serializable value) {
        try {
            String json = mapper.writeValueAsString(value);
            jedis.setex(keyPrefix + key, ttlSeconds, json);
            return key;
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(new java.io.IOException(e));
        }
    }

    @Override
    public String delete(String key) {
        jedis.del(keyPrefix + key);
        return key;
    }
}
