package net.unit8.bouncr.api.repository;

import net.unit8.bouncr.data.WebAuthnCredential;
import org.jooq.DSLContext;
import org.jooq.impl.SQLDataType;

import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.DSL.val;

public class WebAuthnCredentialRepository {
    private final DSLContext dsl;

    public WebAuthnCredentialRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<WebAuthnCredential> findByUserId(Long userId) {
        return dsl.select(
                        field("webauthn_credential_id", Long.class),
                        field("user_id", Long.class),
                        field("credential_id", byte[].class),
                        field("credential_public_key", byte[].class),
                        field("sign_count", Long.class),
                        field("transports", String.class),
                        field("attestation_format", String.class),
                        field("credential_name", String.class),
                        field("discoverable", Boolean.class))
                .from(table("webauthn_credentials"))
                .where(field("user_id").eq(userId))
                .fetch(this::mapCredential);
    }

    public Optional<WebAuthnCredential> findByCredentialId(byte[] credentialId) {
        return dsl.select(
                        field("webauthn_credential_id", Long.class),
                        field("user_id", Long.class),
                        field("credential_id", byte[].class),
                        field("credential_public_key", byte[].class),
                        field("sign_count", Long.class),
                        field("transports", String.class),
                        field("attestation_format", String.class),
                        field("credential_name", String.class),
                        field("discoverable", Boolean.class))
                .from(table("webauthn_credentials"))
                .where(field("credential_id", byte[].class).eq(val(credentialId, SQLDataType.VARBINARY(1024))))
                .fetchOptional(this::mapCredential);
    }

    public WebAuthnCredential insert(Long userId, byte[] credentialId, byte[] credentialPublicKey,
                                     long signCount, String transports, String attestationFormat,
                                     String credentialName, boolean discoverable) {
        var rec = dsl.insertInto(table("webauthn_credentials"),
                        field("user_id"), field("credential_id"), field("credential_public_key"),
                        field("sign_count"), field("transports"), field("attestation_format"),
                        field("credential_name"), field("discoverable"))
                .values(userId, credentialId, credentialPublicKey,
                        signCount, transports, attestationFormat,
                        credentialName, discoverable)
                .returningResult(field("webauthn_credential_id", Long.class))
                .fetchOne();
        if (rec == null) {
            throw new IllegalStateException("Failed to insert WebAuthn credential");
        }
        Long id = rec.get(field("webauthn_credential_id", Long.class));

        return new WebAuthnCredential(id, userId, credentialId, credentialPublicKey,
                signCount, transports, attestationFormat, credentialName, discoverable);
    }

    public void updateSignCount(Long credentialId, long signCount) {
        dsl.update(table("webauthn_credentials"))
                .set(field("sign_count"), (Object) signCount)
                .where(field("webauthn_credential_id").eq(credentialId))
                .execute();
    }

    public void deleteByUserIdAndCredentialId(Long userId, Long credentialId) {
        dsl.deleteFrom(table("webauthn_credentials"))
                .where(field("webauthn_credential_id").eq(credentialId))
                .and(field("user_id").eq(userId))
                .execute();
    }

    private WebAuthnCredential mapCredential(org.jooq.Record r) {
        return new WebAuthnCredential(
                r.get(field("webauthn_credential_id", Long.class)),
                r.get(field("user_id", Long.class)),
                r.get(field("credential_id", byte[].class)),
                r.get(field("credential_public_key", byte[].class)),
                r.get(field("sign_count", Long.class)),
                r.get(field("transports", String.class)),
                r.get(field("attestation_format", String.class)),
                r.get(field("credential_name", String.class)),
                r.get(field("discoverable", Boolean.class)));
    }
}
