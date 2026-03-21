package net.unit8.bouncr.data;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Known action categories emitted to audit logs.
 *
 * <p>Each constant maps to a stable numeric ID and canonical action name used by
 * {@link UserAction}.
 */
public enum ActionType {
    USER_SIGNIN(1L, "user.signin"),
    USER_FAILED_SIGNIN(2L, "user.failed_signin"),
    PASSWORD_CREATED(3L, "user.password_created"),
    PASSWORD_CHANGED(4L, "user.password_changed"),
    PASSWORD_DELETED(5L, "user.password_deleted"),
    USER_CREATED(6L, "user.created"),
    USER_MODIFIED(7L, "user.modified"),
    USER_DELETED(8L, "user.deleted");

    ActionType(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    private String name;
    private Long id;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Resolves an action type from its persistent numeric ID.
     *
     * @param id action type ID
     * @return matching action type
     * @throws java.util.NoSuchElementException when no action type is mapped to the ID
     */
    public static ActionType of(Long id) {
        return Stream.of(ActionType.values())
                .filter(type -> Objects.equals(type.getId(), id))
                .findAny()
                .orElseThrow();
    }
}
