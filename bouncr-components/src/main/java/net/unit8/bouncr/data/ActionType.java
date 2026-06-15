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
    USER_DELETED(8L, "user.deleted"),
    GROUP_CREATED(9L, "group.created"),
    GROUP_MODIFIED(10L, "group.modified"),
    GROUP_DELETED(11L, "group.deleted"),
    ROLE_CREATED(12L, "role.created"),
    ROLE_MODIFIED(13L, "role.modified"),
    ROLE_DELETED(14L, "role.deleted"),
    PERMISSION_CREATED(15L, "permission.created"),
    PERMISSION_MODIFIED(16L, "permission.modified"),
    PERMISSION_DELETED(17L, "permission.deleted"),
    APPLICATION_CREATED(18L, "application.created"),
    APPLICATION_MODIFIED(19L, "application.modified"),
    APPLICATION_DELETED(20L, "application.deleted"),
    REALM_CREATED(21L, "realm.created"),
    REALM_MODIFIED(22L, "realm.modified"),
    REALM_DELETED(23L, "realm.deleted"),
    ASSIGNMENT_CREATED(24L, "assignment.created"),
    ASSIGNMENT_DELETED(25L, "assignment.deleted"),
    USER_SIGNOUT(26L, "user.signout"),
    WEBAUTHN_REGISTERED(27L, "webauthn.registered"),
    WEBAUTHN_DELETED(28L, "webauthn.deleted"),
    OIDC_PROVIDER_CREATED(29L, "oidc_provider.created"),
    OIDC_PROVIDER_MODIFIED(30L, "oidc_provider.modified"),
    OIDC_PROVIDER_DELETED(31L, "oidc_provider.deleted"),
    OIDC_APPLICATION_CREATED(32L, "oidc_application.created"),
    OIDC_APPLICATION_MODIFIED(33L, "oidc_application.modified"),
    OIDC_APPLICATION_DELETED(34L, "oidc_application.deleted"),
    INVITATION_CREATED(35L, "invitation.created"),
    OTP_CREATED(36L, "otp.created"),
    OTP_DELETED(37L, "otp.deleted"),
    GROUP_USER_ADDED(38L, "group.user_added"),
    GROUP_USER_REMOVED(39L, "group.user_removed");

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
