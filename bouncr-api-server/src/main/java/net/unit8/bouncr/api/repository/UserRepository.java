package net.unit8.bouncr.api.repository;

import net.unit8.bouncr.data.*;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static net.unit8.bouncr.api.decoder.BouncrJooqDecoders.*;
import static org.jooq.impl.DSL.*;

public class UserRepository {
    private final DSLContext dsl;

    public UserRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<User> findByAccount(String account) {
        return dsl.select(
                        field("user_id", Long.class),
                        field("account", String.class),
                        field("write_protected", Boolean.class))
                .from(table("users"))
                .where(field("account").eq(account))
                .fetchOptional(rec -> buildUser(rec, false, false, false));
    }

    public Optional<User> findByAccountForSignIn(String account) {
        var userRec = dsl.select(
                        field("user_id", Long.class),
                        field("account", String.class),
                        field("write_protected", Boolean.class))
                .from(table("users"))
                .where(field("account").eq(account))
                .fetchOne();
        if (userRec == null) return Optional.empty();

        User baseUser = USER.decode(userRec).getOrThrow();
        Long userId = baseUser.id();

        PasswordCredential credential = dsl.select(
                        field("password", byte[].class),
                        field("salt", String.class),
                        field("initial", Boolean.class),
                        field("created_at", LocalDateTime.class))
                .from(table("password_credentials"))
                .where(field("user_id").eq(userId))
                .fetchOptional(r -> PASSWORD_CREDENTIAL.decode(r).getOrThrow())
                .orElse(null);

        OtpKey otpKey = dsl.select(field("otp_key", byte[].class))
                .from(table("otp_keys"))
                .where(field("user_id").eq(userId))
                .fetchOptional(r -> new OtpKey(null, r.get(field("otp_key", byte[].class))))
                .orElse(null);

        UserLock userLock = dsl.select(
                        field("lock_level", String.class),
                        field("locked_at", LocalDateTime.class))
                .from(table("user_locks"))
                .where(field("user_id").eq(userId))
                .fetchOptional(r -> new UserLock(
                        null,
                        LockLevel.valueOf(r.get(field("lock_level", String.class))),
                        r.get(field("locked_at", LocalDateTime.class))))
                .orElse(null);

        List<UserProfileValue> profileValues = loadProfileValues(userId);

        return Optional.of(new User(
                userId, baseUser.account(), baseUser.writeProtected(),
                null, profileValues, userLock, credential, otpKey, null, null, null));
    }

    public Optional<User> findById(Long userId) {
        return dsl.select(
                        field("user_id", Long.class),
                        field("account", String.class),
                        field("write_protected", Boolean.class))
                .from(table("users"))
                .where(field("user_id").eq(userId))
                .fetchOptional(rec -> buildUser(rec, false, false, false));
    }

    public Optional<User> findByIdFull(Long userId, boolean embedGroups, boolean embedPermissions) {
        return dsl.select(
                        field("user_id", Long.class),
                        field("account", String.class),
                        field("write_protected", Boolean.class))
                .from(table("users"))
                .where(field("user_id").eq(userId))
                .fetchOptional(rec -> buildUser(rec, embedGroups, embedPermissions, true));
    }

    public List<User> search(String q, Long groupId, Long userId, boolean isAdmin, int offset, int limit) {
        var query = dsl.selectDistinct(
                        field("u.user_id", Long.class).as("user_id"),
                        field("u.account", String.class).as("account"),
                        field("u.write_protected", Boolean.class).as("write_protected"))
                .from(table("users").as("u"));

        var condition = noCondition();
        if (q != null && !q.isEmpty()) {
            String likeExpr = "%" + q.replace("%", "\\%") + "%";
            condition = condition.and(field("u.account", String.class).like(likeExpr));
        }
        if (groupId != null) {
            query = query.join(table("memberships").as("m")).on(field("m.user_id").eq(field("u.user_id")));
            condition = condition.and(field("m.group_id").eq(groupId));
        }
        if (!isAdmin) {
            // Non-admin can only see users in the same groups
            query = query.join(table("memberships").as("my_m")).on(field("my_m.user_id").eq(userId))
                    .join(table("memberships").as("other_m")).on(
                            field("other_m.user_id").eq(field("u.user_id"))
                                    .and(field("other_m.group_id").eq(field("my_m.group_id"))));
        }

        return query.where(condition)
                .orderBy(field("u.user_id").asc())
                .offset(offset)
                .limit(limit)
                .fetch(rec -> buildUser(rec, false, false, true));
    }

    public boolean isAccountUnique(String account) {
        return dsl.selectCount()
                .from(table("users"))
                .where(field("account_lower").eq(account.toLowerCase(Locale.US)))
                .fetchOne(0, int.class) == 0;
    }

    public User insert(String account) {
        Record rec = dsl.insertInto(table("users"),
                        field("account"), field("account_lower"), field("write_protected"))
                .values(account, account.toLowerCase(Locale.US), false)
                .returningResult(
                        field("user_id", Long.class),
                        field("account", String.class),
                        field("write_protected", Boolean.class))
                .fetchOne();
        return USER.decode(rec).getOrThrow();
    }

    public void delete(Long userId) {
        dsl.deleteFrom(table("users"))
                .where(field("user_id").eq(userId))
                .execute();
    }

    public void addToGroup(Long userId, Long groupId) {
        dsl.insertInto(table("memberships"), field("user_id"), field("group_id"))
                .values(userId, groupId)
                .onConflictDoNothing()
                .execute();
    }

    public void removeFromGroup(Long userId, Long groupId) {
        dsl.deleteFrom(table("memberships"))
                .where(field("user_id").eq(userId).and(field("group_id").eq(groupId)))
                .execute();
    }

    public Map<String, List<String>> getPermissionsByRealm(Long userId) {
        return dsl.select(
                        field("a.realm_id", Long.class).as("realm_id"),
                        field("p.name", String.class).as("name"))
                .from(table("assignments").as("a"))
                .join(table("memberships").as("m")).on(field("m.group_id").eq(field("a.group_id")))
                .join(table("role_permissions").as("rp")).on(field("rp.role_id").eq(field("a.role_id")))
                .join(table("permissions").as("p")).on(field("p.permission_id").eq(field("rp.permission_id")))
                .where(field("m.user_id").eq(userId))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        rec -> rec.get(field("realm_id", Long.class)).toString(),
                        Collectors.mapping(
                                rec -> rec.get(field("name", String.class)),
                                Collectors.collectingAndThen(Collectors.toSet(), ArrayList::new))));
    }

    public List<UserProfileValue> loadProfileValues(Long userId) {
        var fId = field("user_profile_field_id", Long.class);
        var fName = field("field_name", String.class);
        var fJsonName = field("json_name", String.class);
        var fIsRequired = field("is_required", Boolean.class);
        var fIsIdentity = field("is_identity", Boolean.class);
        var fRegex = field("regular_expression", String.class);
        var fMaxLen = field("max_length", Integer.class);
        var fMinLen = field("min_length", Integer.class);
        var fNeedsVerification = field("needs_verification", Boolean.class);
        var fPosition = field("field_position", Integer.class);
        var fValue = field("profile_value", String.class);

        return dsl.select(
                        field("upf.user_profile_field_id", Long.class).as("user_profile_field_id"),
                        field("upf.name", String.class).as("field_name"),
                        field("upf.json_name", String.class).as("json_name"),
                        field("upf.is_required", Boolean.class).as("is_required"),
                        field("upf.is_identity", Boolean.class).as("is_identity"),
                        field("upf.regular_expression", String.class).as("regular_expression"),
                        field("upf.max_length", Integer.class).as("max_length"),
                        field("upf.min_length", Integer.class).as("min_length"),
                        field("upf.needs_verification", Boolean.class).as("needs_verification"),
                        field("upf.position", Integer.class).as("field_position"),
                        field("upv.\"value\"", String.class).as("profile_value"))
                .from(table("user_profile_values").as("upv"))
                .join(table("user_profile_fields").as("upf")).on(field("upf.user_profile_field_id").eq(field("upv.user_profile_field_id")))
                .where(field("upv.user_id").eq(userId))
                .fetch(rec -> {
                    UserProfileField upf = new UserProfileField(
                            rec.get(fId),
                            rec.get(fName),
                            rec.get(fJsonName),
                            Boolean.TRUE.equals(rec.get(fIsRequired)),
                            Boolean.TRUE.equals(rec.get(fIsIdentity)),
                            rec.get(fRegex),
                            rec.get(fMaxLen),
                            rec.get(fMinLen),
                            Boolean.TRUE.equals(rec.get(fNeedsVerification)),
                            rec.get(fPosition));
                    return new UserProfileValue(upf, null, rec.get(fValue));
                });
    }

    public void setProfileValue(Long userId, Long fieldId, String value) {
        int updated = dsl.update(table("user_profile_values"))
                .set(field("\"value\""), (Object) value)
                .where(field("user_id").eq(userId).and(field("user_profile_field_id").eq(fieldId)))
                .execute();
        if (updated == 0) {
            dsl.insertInto(table("user_profile_values"),
                            field("user_id"), field("user_profile_field_id"), field("\"value\""))
                    .values(userId, fieldId, value)
                    .execute();
        }
    }

    public void insertPasswordCredential(Long userId, byte[] password, String salt, boolean initial) {
        int updated = dsl.update(table("password_credentials"))
                .set(field("password"), (Object) password)
                .set(field("salt"), (Object) salt)
                .set(field("initial"), (Object) initial)
                .set(field("created_at"), (Object) LocalDateTime.now())
                .where(field("user_id").eq(userId))
                .execute();
        if (updated == 0) {
            dsl.insertInto(table("password_credentials"),
                            field("user_id"), field("password"), field("salt"), field("initial"), field("created_at"))
                    .values(userId, password, salt, initial, LocalDateTime.now())
                    .execute();
        }
    }

    public void deletePasswordCredential(Long userId) {
        dsl.deleteFrom(table("password_credentials"))
                .where(field("user_id").eq(userId))
                .execute();
    }

    public void insertOtpKey(Long userId, byte[] key) {
        int updated = dsl.update(table("otp_keys"))
                .set(field("otp_key"), (Object) key)
                .where(field("user_id").eq(userId))
                .execute();
        if (updated == 0) {
            dsl.insertInto(table("otp_keys"), field("user_id"), field("otp_key"))
                    .values(userId, key)
                    .execute();
        }
    }

    public void deleteOtpKey(Long userId) {
        dsl.deleteFrom(table("otp_keys"))
                .where(field("user_id").eq(userId))
                .execute();
    }

    public void lockUser(Long userId, LockLevel level) {
        int updated = dsl.update(table("user_locks"))
                .set(field("lock_level"), (Object) level.name())
                .set(field("locked_at"), (Object) LocalDateTime.now())
                .where(field("user_id").eq(userId))
                .execute();
        if (updated == 0) {
            dsl.insertInto(table("user_locks"),
                            field("user_id"), field("lock_level"), field("locked_at"))
                    .values(userId, level.name(), LocalDateTime.now())
                    .execute();
        }
    }

    public void unlockUser(Long userId) {
        dsl.deleteFrom(table("user_locks"))
                .where(field("user_id").eq(userId))
                .execute();
    }

    public Optional<Long> findUserIdByProfileIdentity(String jsonName, String value) {
        return dsl.select(field("upv.user_id", Long.class).as("user_id"))
                .from(table("user_profile_values").as("upv"))
                .join(table("user_profile_fields").as("upf")).on(field("upf.user_profile_field_id").eq(field("upv.user_profile_field_id")))
                .where(field("upf.json_name").eq(jsonName)
                        .and(field("upf.is_identity").isTrue())
                        .and(field("upv.\"value\"").eq(value)))
                .fetchOptional(rec -> rec.get(field("user_id", Long.class)));
    }

    public List<Long> findIdsByAccounts(List<String> accounts) {
        return dsl.select(field("user_id", Long.class))
                .from(table("users"))
                .where(field("account").in(accounts))
                .fetch(field("user_id", Long.class));
    }

    public boolean isProfileIdentityConflict(String jsonName, Object value, Long excludeUserId) {
        int cnt = dsl.selectCount()
                .from(table("user_profile_values").as("upv"))
                .join(table("user_profile_fields").as("upf"))
                .on(field("upf.user_profile_field_id").eq(field("upv.user_profile_field_id")))
                .where(field("upf.json_name").eq(jsonName)
                        .and(field("upv.\"value\"").eq(value.toString()))
                        .and(field("upv.user_id").ne(excludeUserId)))
                .fetchOne(0, int.class);
        return cnt > 0;
    }

    public void upsertProfileVerification(Long userId, Long fieldId, String code, java.time.LocalDateTime expiresAt) {
        dsl.insertInto(table("user_profile_verifications"),
                        field("user_profile_field_id"), field("user_id"),
                        field("code"), field("expires_at"))
                .values(fieldId, userId, code, expiresAt)
                .onConflict(field("user_profile_field_id"), field("user_id"))
                .doUpdate()
                .set(field("code"), (Object) code)
                .set(field("expires_at"), (Object) expiresAt)
                .execute();
    }

    public void deleteProfileVerifications(Long userId) {
        dsl.deleteFrom(table("user_profile_verifications"))
                .where(field("user_id").eq(userId))
                .execute();
    }

    public List<OidcUser> loadOidcUsers(Long userId) {
        return dsl.select(
                        field("ou.oidc_provider_id", Long.class).as("oidc_provider_id"),
                        field("ou.oidc_sub", String.class).as("oidc_sub"),
                        field("op.name", String.class).as("name"),
                        field("op.name_lower", String.class).as("name_lower"))
                .from(table("oidc_users").as("ou"))
                .join(table("oidc_providers").as("op")).on(field("op.oidc_provider_id").eq(field("ou.oidc_provider_id")))
                .where(field("ou.user_id").eq(userId))
                .fetch(rec -> new OidcUser(
                        new OidcProvider(
                                rec.get(field("oidc_provider_id", Long.class)),
                                rec.get(field("name", String.class)),
                                rec.get(field("name_lower", String.class)),
                                null, null, null, null, null, null, null, null, null, null, false),
                        null,
                        rec.get(field("oidc_sub", String.class))));
    }

    public List<String> loadUnverifiedProfiles(Long userId) {
        return dsl.select(field("upf.json_name", String.class).as("json_name"))
                .from(table("user_profile_verifications").as("upv"))
                .join(table("user_profile_fields").as("upf"))
                .on(field("upf.user_profile_field_id").eq(field("upv.user_profile_field_id")))
                .where(field("upv.user_id").eq(userId))
                .fetch(rec -> rec.get(field("json_name", String.class)));
    }

    public Optional<OtpKey> findOtpKey(Long userId) {
        return dsl.select(field("otp_key", byte[].class))
                .from(table("otp_keys"))
                .where(field("user_id").eq(userId))
                .fetchOptional(r -> new OtpKey(null, r.get(field("otp_key", byte[].class))));
    }

    public boolean isLocked(Long userId) {
        return dsl.selectCount()
                .from(table("user_locks"))
                .where(field("user_id").eq(userId))
                .fetchOne(0, int.class) > 0;
    }

    public Optional<OidcUser> findOidcUser(Long oidcProviderId, String sub) {
        var rec = dsl.select(
                        field("ou.user_id", Long.class).as("user_id"),
                        field("ou.oidc_sub", String.class).as("oidc_sub"),
                        field("u.account", String.class).as("account"),
                        field("u.write_protected", Boolean.class).as("write_protected"))
                .from(table("oidc_users").as("ou"))
                .join(table("users").as("u")).on(field("u.user_id").eq(field("ou.user_id")))
                .where(field("ou.oidc_sub").eq(sub)
                        .and(field("ou.oidc_provider_id").eq(oidcProviderId)))
                .fetchOne();
        if (rec == null) return Optional.empty();

        User user = new User(
                rec.get(field("user_id", Long.class)),
                rec.get(field("account", String.class)),
                rec.get(field("write_protected", Boolean.class)),
                null, null, null, null, null, null, null, null);
        return Optional.of(new OidcUser(null, user, rec.get(field("oidc_sub", String.class))));
    }

    public void insertOidcUser(Long oidcProviderId, Long userId, String sub) {
        dsl.insertInto(table("oidc_users"),
                        field("oidc_provider_id"), field("user_id"), field("oidc_sub"))
                .values(oidcProviderId, userId, sub)
                .execute();
    }

    public void insertProfileVerification(Long fieldId, Long userId, String code, java.time.LocalDateTime expiresAt) {
        dsl.insertInto(table("user_profile_verifications"),
                        field("user_profile_field_id"), field("user_id"),
                        field("code"), field("expires_at"))
                .values(fieldId, userId, code, expiresAt)
                .execute();
    }

    private User buildUser(Record rec, boolean embedGroups, boolean embedPermissions, boolean loadProfiles) {
        User baseUser = USER.decode(rec).getOrThrow();
        Long userId = baseUser.id();
        List<Group> groups = embedGroups ? loadGroups(userId) : null;
        List<UserProfileValue> profileValues = loadProfiles ? loadProfileValues(userId) : null;
        List<String> permissions = embedPermissions ? loadPermissions(userId) : null;

        return new User(
                userId, baseUser.account(), baseUser.writeProtected(),
                groups, profileValues, null, null, null, null, permissions, null);
    }

    private List<Group> loadGroups(Long userId) {
        return dsl.select(
                        field("g.group_id", Long.class).as("group_id"),
                        field("g.name", String.class).as("name"),
                        field("g.description", String.class).as("description"),
                        field("g.write_protected", Boolean.class).as("write_protected"))
                .from(table("memberships").as("m"))
                .join(table("groups").as("g")).on(field("g.group_id").eq(field("m.group_id")))
                .where(field("m.user_id").eq(userId))
                .fetch(r -> GROUP.decode(r).getOrThrow());
    }

    private List<String> loadPermissions(Long userId) {
        return dsl.selectDistinct(field("p.name", String.class).as("name"))
                .from(table("memberships").as("m"))
                .join(table("assignments").as("a")).on(field("a.group_id").eq(field("m.group_id")))
                .join(table("role_permissions").as("rp")).on(field("rp.role_id").eq(field("a.role_id")))
                .join(table("permissions").as("p")).on(field("p.permission_id").eq(field("rp.permission_id")))
                .where(field("m.user_id").eq(userId))
                .fetch(rec -> rec.get(field("name", String.class)));
    }
}
