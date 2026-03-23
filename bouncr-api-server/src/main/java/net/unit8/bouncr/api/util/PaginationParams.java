package net.unit8.bouncr.api.util;

/**
 * Safe parsing utilities for pagination query parameters.
 *
 * <p>All methods return sensible defaults when the input is null, non-numeric,
 * or out of range. This eliminates the {@code NumberFormatException} risk of
 * using {@code Integer::parseInt} / {@code Long::parseLong} directly on
 * user-supplied query parameters.
 */
public final class PaginationParams {

    private PaginationParams() {}

    /**
     * Parse an offset parameter. Returns 0 for null, non-numeric, or negative values.
     */
    public static int parseOffset(String value) {
        if (value == null) return 0;
        try {
            int v = Integer.parseInt(value);
            return Math.max(v, 0);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Parse a limit parameter. Returns {@code defaultLimit} for null, non-numeric,
     * non-positive, or values exceeding 1000.
     */
    public static int parseLimit(String value, int defaultLimit) {
        if (value == null) return defaultLimit;
        try {
            int v = Integer.parseInt(value);
            return (v > 0 && v <= 1000) ? v : defaultLimit;
        } catch (NumberFormatException e) {
            return defaultLimit;
        }
    }

    /**
     * Parse a Long parameter (e.g., group_id). Returns null for null or non-numeric values.
     */
    public static Long parseLong(String value) {
        if (value == null) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
