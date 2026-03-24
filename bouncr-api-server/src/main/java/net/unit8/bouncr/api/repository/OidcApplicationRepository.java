package net.unit8.bouncr.api.repository;

import net.unit8.bouncr.data.GrantType;
import net.unit8.bouncr.data.OidcApplication;
import net.unit8.bouncr.data.Permission;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.unit8.bouncr.api.decoder.BouncrJooqDecoders.OIDC_APPLICATION;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static net.unit8.bouncr.api.decoder.BouncrJooqDecoders.*;
import static org.jooq.impl.DSL.*;

public class OidcApplicationRepository {
    private static final Logger LOG = LoggerFactory.getLogger(OidcApplicationRepository.class);
    private final DSLContext dsl;

    public OidcApplicationRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<OidcApplication> findByName(String name) {
        var rec = dsl.select(
                        field("oidc_application_id", Long.class),
                        field("name", String.class),
                        field("name_lower", String.class),
                        field("client_id", String.class),
                        field("client_secret", String.class),
                        field("private_key", byte[].class),
                        field("public_key", byte[].class),
                        field("home_uri", String.class),
                        field("callback_uri", String.class),
                        field("description", String.class),
                        field("backchannel_logout_uri", String.class),
                        field("frontchannel_logout_uri", String.class))
                .from(table("oidc_applications"))
                .where(field("name").eq(name))
                .fetchOne();
        if (rec == null) return Optional.empty();

        OidcApplication app = mapOidcApplication(rec);
        Long appId = app.id();
        List<Permission> permissions = findPermissionsByOidcApplicationId(appId);
        Set<GrantType> grantTypes = loadGrantTypes(appId);
        return Optional.of(new OidcApplication(app.id(), app.name(), app.nameLower(),
                app.clientId(), app.clientSecret(), app.privateKey(), app.publicKey(),
                app.homeUri(), app.callbackUri(), app.description(),
                app.backchannelLogoutUri(), app.frontchannelLogoutUri(), permissions, grantTypes));
    }

    /**
     * Find public key only by client_id (for JWKS endpoint — no secrets loaded).
     */
    public Optional<byte[]> findPublicKeyByClientId(String clientId) {
        var rec = dsl.select(field("public_key", byte[].class))
                .from(table("oidc_applications"))
                .where(field("client_id").eq(clientId))
                .fetchOne();
        if (rec == null) return Optional.empty();
        return Optional.ofNullable(rec.get(field("public_key", byte[].class)));
    }

    public Optional<OidcApplication> findByClientId(String clientId) {
        var rec = dsl.select(
                        field("oidc_application_id", Long.class),
                        field("name", String.class),
                        field("name_lower", String.class),
                        field("client_id", String.class),
                        field("client_secret", String.class),
                        field("private_key", byte[].class),
                        field("public_key", byte[].class),
                        field("home_uri", String.class),
                        field("callback_uri", String.class),
                        field("description", String.class),
                        field("backchannel_logout_uri", String.class),
                        field("frontchannel_logout_uri", String.class))
                .from(table("oidc_applications"))
                .where(field("client_id").eq(clientId))
                .fetchOne();
        if (rec == null) return Optional.empty();

        OidcApplication app = mapOidcApplication(rec);
        Long appId = app.id();
        List<Permission> permissions = findPermissionsByOidcApplicationId(appId);
        Set<GrantType> grantTypes = loadGrantTypes(appId);
        return Optional.of(new OidcApplication(app.id(), app.name(), app.nameLower(),
                app.clientId(), app.clientSecret(), app.privateKey(), app.publicKey(),
                app.homeUri(), app.callbackUri(), app.description(),
                app.backchannelLogoutUri(), app.frontchannelLogoutUri(), permissions, grantTypes));
    }

    public List<OidcApplication> search(String q, int offset, int limit) {
        var condition = noCondition();
        if (q != null && !q.isEmpty()) {
            condition = condition.and(LikeQuery.contains(field("name", String.class), q));
        }

        List<OidcApplication> apps = dsl.select(
                        field("oidc_application_id", Long.class),
                        field("name", String.class),
                        field("name_lower", String.class),
                        field("client_id", String.class),
                        field("client_secret", String.class),
                        field("private_key", byte[].class),
                        field("public_key", byte[].class),
                        field("home_uri", String.class),
                        field("callback_uri", String.class),
                        field("description", String.class),
                        field("backchannel_logout_uri", String.class),
                        field("frontchannel_logout_uri", String.class))
                .from(table("oidc_applications"))
                .where(condition)
                .orderBy(field("oidc_application_id").asc())
                .offset(offset)
                .limit(limit)
                .fetch(this::mapOidcApplication);
        return attachGrantTypes(apps);
    }

    public List<OidcApplication> listAll() {
        List<OidcApplication> apps = dsl.select(
                        field("oidc_application_id", Long.class),
                        field("name", String.class),
                        field("name_lower", String.class),
                        field("client_id", String.class),
                        field("client_secret", String.class),
                        field("private_key", byte[].class),
                        field("public_key", byte[].class),
                        field("home_uri", String.class),
                        field("callback_uri", String.class),
                        field("description", String.class),
                        field("backchannel_logout_uri", String.class),
                        field("frontchannel_logout_uri", String.class))
                .from(table("oidc_applications"))
                .orderBy(field("oidc_application_id").asc())
                .fetch(this::mapOidcApplication);
        return attachGrantTypes(apps);
    }

    public boolean isNameUnique(String name) {
        return dsl.selectCount()
                .from(table("oidc_applications"))
                .where(field("name_lower").eq(name.toLowerCase(Locale.US)))
                .fetchOne(0, int.class) == 0;
    }

    public OidcApplication insert(String name, String clientId, String clientSecret,
                                  byte[] privateKey, byte[] publicKey,
                                  String homeUri, String callbackUri, String description,
                                  String backchannelLogoutUri, String frontchannelLogoutUri) {
        dsl.insertInto(table("oidc_applications"),
                        field("name"), field("name_lower"), field("client_id"), field("client_secret"),
                        field("private_key"), field("public_key"),
                        field("home_uri"), field("callback_uri"), field("description"),
                        field("backchannel_logout_uri"), field("frontchannel_logout_uri"))
                .values(name, name.toLowerCase(Locale.US), clientId, clientSecret,
                        privateKey, publicKey, homeUri, callbackUri, description,
                        backchannelLogoutUri, frontchannelLogoutUri)
                .execute();
        return findByName(name).orElseThrow();
    }

    /**
     * Nullable field update: present = set to value (including null to clear), absent = leave unchanged.
     */
    public record NullableUpdate<T>(boolean present, T value) {
        public static <T> NullableUpdate<T> of(T value) { return new NullableUpdate<>(true, value); }
        public static <T> NullableUpdate<T> absent() { return new NullableUpdate<>(false, null); }
    }

    /**
     * Update profile fields of an OIDC application.
     * Nullable fields use {@link NullableUpdate} to distinguish "set to null" from "leave unchanged".
     */
    public void updateProfile(String currentName, String newName,
                              NullableUpdate<String> homeUri,
                              NullableUpdate<String> callbackUri,
                              NullableUpdate<String> description,
                              NullableUpdate<String> backchannelLogoutUri,
                              NullableUpdate<String> frontchannelLogoutUri) {
        var updateSet = dsl.update(table("oidc_applications"))
                .set(field("name"), (Object) (newName != null ? newName : field("name")));
        if (newName != null) {
            updateSet = updateSet.set(field("name_lower"), (Object) newName.toLowerCase(Locale.US));
        }
        if (homeUri.present()) {
            updateSet = updateSet.set(field("home_uri"), (Object) homeUri.value());
        }
        if (callbackUri.present()) {
            updateSet = updateSet.set(field("callback_uri"), (Object) callbackUri.value());
        }
        if (description.present()) {
            updateSet = updateSet.set(field("description"), (Object) description.value());
        }
        if (backchannelLogoutUri.present()) {
            updateSet = updateSet.set(field("backchannel_logout_uri"), (Object) backchannelLogoutUri.value());
        }
        if (frontchannelLogoutUri.present()) {
            updateSet = updateSet.set(field("frontchannel_logout_uri"), (Object) frontchannelLogoutUri.value());
        }
        updateSet.where(field("name").eq(currentName))
                .execute();
    }

    /**
     * Update only the client_secret (for secret regeneration).
     */
    public void updateClientSecret(String name, String hashedSecret) {
        dsl.update(table("oidc_applications"))
                .set(field("client_secret"), (Object) hashedSecret)
                .where(field("name").eq(name))
                .execute();
    }

    public void setPermissions(Long oidcApplicationId, List<String> permissionNames) {
        // Remove existing scopes
        dsl.deleteFrom(table("oidc_application_scopes"))
                .where(field("oidc_application_id").eq(oidcApplicationId))
                .execute();

        if (permissionNames != null && !permissionNames.isEmpty()) {
            // Look up permission IDs by name
            var permissions = dsl.select(field("permission_id", Long.class))
                    .from(table("permissions"))
                    .where(field("name").in(permissionNames))
                    .fetch(rec -> rec.get(field("permission_id", Long.class)));

            for (Long permissionId : permissions) {
                dsl.insertInto(table("oidc_application_scopes"),
                                field("oidc_application_id"), field("permission_id"))
                        .values(oidcApplicationId, permissionId)
                        .execute();
            }
        }
    }

    public Set<GrantType> loadGrantTypes(Long oidcApplicationId) {
        List<String> values = dsl.select(field("grant_type", String.class))
                .from(table("oidc_application_grant_types"))
                .where(field("oidc_application_id").eq(oidcApplicationId))
                .fetch(rec -> rec.get(field("grant_type", String.class)));
        if (values.isEmpty()) return null;
        EnumSet<GrantType> result = EnumSet.noneOf(GrantType.class);
        for (String v : values) {
            GrantType.fromString(v).ifPresent(result::add);
        }
        return result;
    }

    /**
     * Bulk-load grant types for a list of applications (avoids N+1).
     */
    private Map<Long, Set<GrantType>> loadGrantTypesForApps(List<Long> appIds) {
        if (appIds.isEmpty()) return Map.of();
        var rows = dsl.select(field("oidc_application_id", Long.class), field("grant_type", String.class))
                .from(table("oidc_application_grant_types"))
                .where(field("oidc_application_id").in(appIds))
                .fetch();
        Map<Long, Set<GrantType>> result = new HashMap<>();
        for (var rec : rows) {
            Long appId = rec.get(field("oidc_application_id", Long.class));
            String gtStr = rec.get(field("grant_type", String.class));
            GrantType.fromString(gtStr).ifPresent(gt ->
                    result.computeIfAbsent(appId, k -> EnumSet.noneOf(GrantType.class)).add(gt));
        }
        return result;
    }

    public void setGrantTypes(Long oidcApplicationId, Set<GrantType> grantTypes) {
        dsl.deleteFrom(table("oidc_application_grant_types"))
                .where(field("oidc_application_id").eq(oidcApplicationId))
                .execute();
        for (GrantType gt : grantTypes) {
            dsl.insertInto(table("oidc_application_grant_types"),
                            field("oidc_application_id"), field("grant_type"))
                    .values(oidcApplicationId, gt.getValue())
                    .execute();
        }
    }

    public void delete(String name) {
        dsl.deleteFrom(table("oidc_applications"))
                .where(field("name").eq(name))
                .execute();
    }

    private List<Permission> findPermissionsByOidcApplicationId(Long oidcApplicationId) {
        return dsl.select(
                        field("p.permission_id", Long.class).as("permission_id"),
                        field("p.name", String.class).as("name"),
                        field("p.description", String.class).as("description"),
                        field("p.write_protected", Boolean.class).as("write_protected"))
                .from(table("permissions").as("p"))
                .join(table("oidc_application_scopes").as("oas"))
                .on(field("oas.permission_id").eq(field("p.permission_id")))
                .where(field("oas.oidc_application_id").eq(oidcApplicationId))
                .fetch(rec -> PERMISSION.decode(rec).getOrThrow());
    }

    private List<OidcApplication> attachGrantTypes(List<OidcApplication> apps) {
        List<Long> ids = apps.stream().map(OidcApplication::id).filter(id -> id != null).toList();
        Map<Long, Set<GrantType>> grantMap = loadGrantTypesForApps(ids);
        return apps.stream().map(app -> {
            Set<GrantType> gts = grantMap.get(app.id());
            return new OidcApplication(app.id(), app.name(), app.nameLower(),
                    app.clientId(), app.clientSecret(), app.privateKey(), app.publicKey(),
                    app.homeUri(), app.callbackUri(), app.description(),
                    app.backchannelLogoutUri(), app.frontchannelLogoutUri(),
                    app.permissions(), gts);
        }).toList();
    }

    private OidcApplication mapOidcApplication(Record rec) {
        return OIDC_APPLICATION.decode(rec).getOrThrow();
    }
}
