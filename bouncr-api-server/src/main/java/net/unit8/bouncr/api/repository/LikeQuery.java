package net.unit8.bouncr.api.repository;

import org.jooq.Condition;
import org.jooq.Field;

/**
 * LIKE query helper with proper escaping of wildcard characters.
 */
final class LikeQuery {
    private LikeQuery() {}

    /**
     * Creates a contains-match LIKE condition with escaped wildcards.
     * Escapes both {@code %} and {@code _} in the search term using backslash,
     * and declares the escape character via SQL ESCAPE clause.
     */
    static Condition contains(Field<String> field, String term) {
        String escaped = term.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return field.like("%" + escaped + "%").escape('\\');
    }
}
