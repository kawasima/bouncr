package net.unit8.bouncr.api.repository;

import net.unit8.bouncr.data.OidcProvider;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static net.unit8.bouncr.api.decoder.BouncrJooqDecoders.*;
import static org.jooq.impl.DSL.*;

public class OidcProviderRepository {
    private final DSLContext dsl;

    public OidcProviderRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<OidcProvider> findByName(String name) {
        var rec = dsl.select(
                        field("oidc_provider_id", Long.class),
                        field("name", String.class),
                        field("name_lower", String.class),
                        field("client_id", String.class),
                        field("client_secret", String.class),
                        field("scope", String.class),
                        field("response_type", String.class),
                        field("token_endpoint", String.class),
                        field("authorization_endpoint", String.class),
                        field("token_endpoint_auth_method", String.class),
                        field("redirect_uri", String.class),
                        field("jwks_uri", String.class),
                        field("issuer", String.class),
                        field("pkce_enabled", Boolean.class))
                .from(table("oidc_providers"))
                .where(field("name").eq(name))
                .fetchOne();
        if (rec == null) return Optional.empty();

        return Optional.of(OIDC_PROVIDER.decode(rec).getOrThrow());
    }

    public List<OidcProvider> search(String q, int offset, int limit) {
        var condition = noCondition();
        if (q != null && !q.isEmpty()) {
            String likeExpr = "%" + q.replace("%", "\\%") + "%";
            condition = condition.and(field("name", String.class).like(likeExpr));
        }

        return dsl.select(
                        field("oidc_provider_id", Long.class),
                        field("name", String.class),
                        field("name_lower", String.class),
                        field("client_id", String.class),
                        field("client_secret", String.class),
                        field("scope", String.class),
                        field("response_type", String.class),
                        field("token_endpoint", String.class),
                        field("authorization_endpoint", String.class),
                        field("token_endpoint_auth_method", String.class),
                        field("redirect_uri", String.class),
                        field("jwks_uri", String.class),
                        field("issuer", String.class),
                        field("pkce_enabled", Boolean.class))
                .from(table("oidc_providers"))
                .where(condition)
                .orderBy(field("oidc_provider_id").asc())
                .offset(offset)
                .limit(limit)
                .fetch(rec -> OIDC_PROVIDER.decode(rec).getOrThrow());
    }

    public boolean isNameUnique(String name) {
        return dsl.selectCount()
                .from(table("oidc_providers"))
                .where(field("name_lower").eq(name.toLowerCase(Locale.US)))
                .fetchOne(0, int.class) == 0;
    }

    public OidcProvider insert(String name, String clientId, String clientSecret,
                               String scope, String responseType, String tokenEndpoint,
                               String authorizationEndpoint, String tokenEndpointAuthMethod,
                               String redirectUri, String jwksUri, String issuer, boolean pkceEnabled) {
        Record rec = dsl.insertInto(table("oidc_providers"),
                        field("name"), field("name_lower"), field("client_id"), field("client_secret"),
                        field("scope"), field("response_type"), field("token_endpoint"),
                        field("authorization_endpoint"), field("token_endpoint_auth_method"),
                        field("redirect_uri"), field("jwks_uri"), field("issuer"), field("pkce_enabled"))
                .values(name, name.toLowerCase(Locale.US), clientId, clientSecret,
                        scope, responseType, tokenEndpoint,
                        authorizationEndpoint, tokenEndpointAuthMethod,
                        redirectUri, jwksUri, issuer, pkceEnabled)
                .returningResult(
                        field("oidc_provider_id", Long.class),
                        field("name", String.class),
                        field("name_lower", String.class),
                        field("client_id", String.class),
                        field("client_secret", String.class),
                        field("scope", String.class),
                        field("response_type", String.class),
                        field("token_endpoint", String.class),
                        field("authorization_endpoint", String.class),
                        field("token_endpoint_auth_method", String.class),
                        field("redirect_uri", String.class),
                        field("jwks_uri", String.class),
                        field("issuer", String.class),
                        field("pkce_enabled", Boolean.class))
                .fetchOne();
        return OIDC_PROVIDER.decode(rec).getOrThrow();
    }

    public void update(String currentName, String newName, String clientId, String clientSecret,
                       String scope, String responseType, String tokenEndpoint,
                       String authorizationEndpoint, String tokenEndpointAuthMethod,
                       String redirectUri, String jwksUri, String issuer, Boolean pkceEnabled) {
        var updateSet = dsl.update(table("oidc_providers"))
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
        if (scope != null) {
            updateSet = updateSet.set(field("scope"), (Object) scope);
        }
        if (responseType != null) {
            updateSet = updateSet.set(field("response_type"), (Object) responseType);
        }
        if (tokenEndpoint != null) {
            updateSet = updateSet.set(field("token_endpoint"), (Object) tokenEndpoint);
        }
        if (authorizationEndpoint != null) {
            updateSet = updateSet.set(field("authorization_endpoint"), (Object) authorizationEndpoint);
        }
        if (tokenEndpointAuthMethod != null) {
            updateSet = updateSet.set(field("token_endpoint_auth_method"), (Object) tokenEndpointAuthMethod);
        }
        if (redirectUri != null) {
            updateSet = updateSet.set(field("redirect_uri"), (Object) redirectUri);
        }
        if (jwksUri != null) {
            updateSet = updateSet.set(field("jwks_uri"), (Object) jwksUri);
        }
        if (issuer != null) {
            updateSet = updateSet.set(field("issuer"), (Object) issuer);
        }
        if (pkceEnabled != null) {
            updateSet = updateSet.set(field("pkce_enabled"), (Object) pkceEnabled);
        }
        updateSet.where(field("name").eq(currentName))
                .execute();
    }

    public void delete(String name) {
        dsl.deleteFrom(table("oidc_providers"))
                .where(field("name").eq(name))
                .execute();
    }
}
