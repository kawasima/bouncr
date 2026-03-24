package net.unit8.bouncr.api.util;

import kotowari.restful.data.ContextKey;

/**
 * Utility for creating {@link ContextKey} instances with parameterized types.
 *
 * <p>Centralizes the unchecked cast required when creating a ContextKey for
 * a generic type (e.g., {@code Tuple2<WordName, String>}), since
 * {@code ContextKey.of(Tuple2.class)} erases the type parameter.
 */
public final class ContextKeys {
    private ContextKeys() {}

    @SuppressWarnings("unchecked")
    public static <T> ContextKey<T> of(Class<?> rawClass) {
        return (ContextKey<T>) (ContextKey<?>) ContextKey.of(rawClass);
    }

    @SuppressWarnings("unchecked")
    public static <T> ContextKey<T> of(String name, Class<?> rawClass) {
        return (ContextKey<T>) (ContextKey<?>) ContextKey.of(name, rawClass);
    }
}
