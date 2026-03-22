package net.unit8.bouncr.api.repository;

import net.unit8.bouncr.data.OidcApplication;
import net.unit8.bouncr.data.Permission;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static net.unit8.bouncr.api.decoder.BouncrJooqDecoders.*;
import static org.jooq.impl.DSL.*;

public class OidcApplicationRepository {
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
                        field("home_url", String.class),
                        field("callback_url", String.class),
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
        return Optional.of(new OidcApplication(app.id(), app.name(), app.nameLower(),
                app.clientId(), app.clientSecret(), app.privateKey(), app.publicKey(),
                app.homeUrl(), app.callbackUrl(), app.description(),
                app.backchannelLogoutUri(), app.frontchannelLogoutUri(), permissions));
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
                        field("home_url", String.class),
                        field("callback_url", String.class),
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
        return Optional.of(new OidcApplication(app.id(), app.name(), app.nameLower(),
                app.clientId(), app.clientSecret(), app.privateKey(), app.publicKey(),
                app.homeUrl(), app.callbackUrl(), app.description(),
                app.backchannelLogoutUri(), app.frontchannelLogoutUri(), permissions));
    }

    public List<OidcApplication> search(String q, int offset, int limit) {
        var condition = noCondition();
        if (q != null && !q.isEmpty()) {
            String likeExpr = "%" + q.replace("%", "\\%") + "%";
            condition = condition.and(field("name", String.class).like(likeExpr));
        }

        return dsl.select(
                        field("oidc_application_id", Long.class),
                        field("name", String.class),
                        field("name_lower", String.class),
                        field("client_id", String.class),
                        field("client_secret", String.class),
                        field("private_key", byte[].class),
                        field("public_key", byte[].class),
                        field("home_url", String.class),
                        field("callback_url", String.class),
                        field("description", String.class),
                        field("backchannel_logout_uri", String.class),
                        field("frontchannel_logout_uri", String.class))
                .from(table("oidc_applications"))
                .where(condition)
                .orderBy(field("oidc_application_id").asc())
                .offset(offset)
                .limit(limit)
                .fetch(this::mapOidcApplication);
    }

    public List<OidcApplication> listAll() {
        return dsl.select(
                        field("oidc_application_id", Long.class),
                        field("name", String.class),
                        field("name_lower", String.class),
                        field("client_id", String.class),
                        field("client_secret", String.class),
                        field("private_key", byte[].class),
                        field("public_key", byte[].class),
                        field("home_url", String.class),
                        field("callback_url", String.class),
                        field("description", String.class),
                        field("backchannel_logout_uri", String.class),
                        field("frontchannel_logout_uri", String.class))
                .from(table("oidc_applications"))
                .orderBy(field("oidc_application_id").asc())
                .fetch(this::mapOidcApplication);
    }

    public boolean isNameUnique(String name) {
        return dsl.selectCount()
                .from(table("oidc_applications"))
                .where(field("name_lower").eq(name.toLowerCase(Locale.US)))
                .fetchOne(0, int.class) == 0;
    }

    public OidcApplication insert(String name, String clientId, String clientSecret,
                                  byte[] privateKey, byte[] publicKey,
                                  String homeUrl, String callbackUrl, String description,
                                  String backchannelLogoutUri, String frontchannelLogoutUri) {
        dsl.insertInto(table("oidc_applications"),
                        field("name"), field("name_lower"), field("client_id"), field("client_secret"),
                        field("private_key"), field("public_key"),
                        field("home_url"), field("callback_url"), field("description"),
                        field("backchannel_logout_uri"), field("frontchannel_logout_uri"))
                .values(name, name.toLowerCase(Locale.US), clientId, clientSecret,
                        privateKey, publicKey, homeUrl, callbackUrl, description,
                        backchannelLogoutUri, frontchannelLogoutUri)
                .execute();
        return findByName(name).orElseThrow();
    }

    public void update(String currentName, String newName, String clientId, String clientSecret,
                       byte[] privateKey, byte[] publicKey,
                       String homeUrl, String callbackUrl, String description,
                       String backchannelLogoutUri, String frontchannelLogoutUri) {
        var updateSet = dsl.update(table("oidc_applications"))
                .set(field("name"), (Object) (newName != null ? newName : field("name")));
        if (newName != null) {
            updateSet = updateSet.set(field("name_lower"), (Object) newName.toLowerCase(Locale.US));
        }
        if (clientId != null) {
            updateSet = updateSet.set(field("client_id"), (Object) clientId);
        }
        if (clientSecret != null) {
            updateSet = updateSet.set(field("client_secret"), (Object) clientSecret);
        }
        if (privateKey != null) {
            updateSet = updateSet.set(field("private_key"), (Object) privateKey);
        }
        if (publicKey != null) {
            updateSet = updateSet.set(field("public_key"), (Object) publicKey);
        }
        if (homeUrl != null) {
            updateSet = updateSet.set(field("home_url"), (Object) homeUrl);
        }
        if (callbackUrl != null) {
            updateSet = updateSet.set(field("callback_url"), (Object) callbackUrl);
        }
        if (description != null) {
            updateSet = updateSet.set(field("description"), (Object) description);
        }
        if (backchannelLogoutUri != null) {
            updateSet = updateSet.set(field("backchannel_logout_uri"), (Object) backchannelLogoutUri);
        }
        if (frontchannelLogoutUri != null) {
            updateSet = updateSet.set(field("frontchannel_logout_uri"), (Object) frontchannelLogoutUri);
        }
        updateSet.where(field("name").eq(currentName))
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

    private OidcApplication mapOidcApplication(Record rec) {
        try {
            String homeUrl = rec.get(field("home_url", String.class));
            String callbackUrl = rec.get(field("callback_url", String.class));
            String backchannelLogoutUri = rec.get(field("backchannel_logout_uri", String.class));
            String frontchannelLogoutUri = rec.get(field("frontchannel_logout_uri", String.class));

            return new OidcApplication(
                    rec.get(field("oidc_application_id", Long.class)),
                    rec.get(field("name", String.class)),
                    rec.get(field("name_lower", String.class)),
                    rec.get(field("client_id", String.class)),
                    rec.get(field("client_secret", String.class)),
                    rec.get(field("private_key", byte[].class)),
                    rec.get(field("public_key", byte[].class)),
                    homeUrl != null ? URI.create(homeUrl).toURL() : null,
                    callbackUrl != null ? URI.create(callbackUrl).toURL() : null,
                    rec.get(field("description", String.class)),
                    backchannelLogoutUri != null ? URI.create(backchannelLogoutUri).toURL() : null,
                    frontchannelLogoutUri != null ? URI.create(frontchannelLogoutUri).toURL() : null,
                    null
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to map OidcApplication record", e);
        }
    }
}
