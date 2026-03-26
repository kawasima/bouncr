package db.migration;

import enkan.Env;
import net.unit8.bouncr.data.ActionType;
import net.unit8.bouncr.util.PasswordUtils;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.migration.Context;
import org.flywaydb.core.api.migration.JavaMigration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;

import static org.jooq.impl.DSL.constraint;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

public class B28__BouncrV0_3_0 implements JavaMigration {
    private static final Logger LOG = LoggerFactory.getLogger(B28__BouncrV0_3_0.class);

    private static final String[] ADMIN_PERMISSIONS = new String[]{
            "any_user:read", "any_user:create", "any_user:update", "any_user:delete",
            "any_user:lock", "any_user:unlock",
            "any_group:read", "any_group:create", "any_group:update", "any_group:delete",
            "any_application:read", "any_application:create", "any_application:update", "any_application:delete",
            "any_realm:read", "any_realm:create", "any_realm:update", "any_realm:delete",
            "any_role:read", "any_role:create", "any_role:update", "any_role:delete",
            "any_permission:read", "any_permission:create", "any_permission:update", "any_permission:delete",
            "assignments:read", "assignments:create", "assignments:delete",
            "oidc_application:read", "oidc_application:create", "oidc_application:update", "oidc_application:delete",
            "oidc_provider:read", "oidc_provider:create", "oidc_provider:update", "oidc_provider:delete",
            "invitation:create"
    };

    private static final String[] OTHER_PERMISSIONS = new String[]{
            "user:read", "user:create", "user:update", "user:delete",
            "user:lock", "user:unlock",
            "group:read", "group:create", "group:update", "group:delete",
            "application:read", "application:create", "application:update", "application:delete",
            "realm:read", "realm:create", "realm:update", "realm:delete",
            "role:read", "role:create", "role:update", "role:delete",
            "permission:read", "permission:create", "permission:update", "permission:delete"
    };

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        DSLContext create = DSL.using(connection);

        createSchema(connection, create);
        seedActions(connection);
        seedAdminData(connection);
    }

    @Override
    public MigrationVersion getVersion() {
        return MigrationVersion.fromVersion("28");
    }

    @Override
    public String getDescription() {
        return "BouncrV0_3_0";
    }

    @Override
    public Integer getChecksum() {
        return null;
    }

    @Override
    public boolean canExecuteInTransaction() {
        return true;
    }

    private void createSchema(Connection connection, DSLContext create) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(create.createTable(table("users"))
                    .column(field("user_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("account", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("write_protected", SQLDataType.BOOLEAN.nullable(false)))
                    .column(field("account_lower", SQLDataType.VARCHAR(100).nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("user_id")),
                            constraint().unique(field("account"))
                    ).getSQL());

            stmt.execute(create.createIndex(name("idx_users_01"))
                    .on(table("users"), field("account"))
                    .getSQL());

            stmt.execute(create.createTable(table("groups"))
                    .column(field("group_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("name", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("description", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("write_protected", SQLDataType.BOOLEAN.nullable(false)))
                    .column(field("name_lower", SQLDataType.VARCHAR(100).nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("group_id")),
                            constraint().unique(field("name"))
                    ).getSQL());

            stmt.execute(create.createTable(table("applications"))
                    .column(field("application_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("name", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("virtual_path", SQLDataType.VARCHAR(50).nullable(false)))
                    .column(field("pass_to", SQLDataType.VARCHAR(255).nullable(false)))
                    .column(field("top_page", SQLDataType.VARCHAR(255).nullable(false)))
                    .column(field("description", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("write_protected", SQLDataType.BOOLEAN.nullable(false)))
                    .column(field("name_lower", SQLDataType.VARCHAR(100).nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("application_id")),
                            constraint().unique(field("name")),
                            constraint().unique(field("virtual_path"))
                    ).getSQL());

            stmt.execute(create.createTable(table("roles"))
                    .column(field("role_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("name", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("description", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("write_protected", SQLDataType.BOOLEAN.nullable(false)))
                    .column(field("name_lower", SQLDataType.VARCHAR(100).nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("role_id")),
                            constraint().unique(field("name"))
                    ).getSQL());

            stmt.execute(create.createTable(table("permissions"))
                    .column(field("permission_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("name", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("description", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("write_protected", SQLDataType.BOOLEAN.nullable(false)))
                    .column(field("name_lower", SQLDataType.VARCHAR(100).nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("permission_id")),
                            constraint().unique(field("name"))
                    ).getSQL());

            stmt.execute(create.createTable(table("realms"))
                    .column(field("realm_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("name", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("url", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("application_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("description", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("write_protected", SQLDataType.BOOLEAN.nullable(false)))
                    .column(field("name_lower", SQLDataType.VARCHAR(100).nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("realm_id")),
                            constraint().unique(field("name")),
                            constraint().foreignKey(field("application_id"))
                                    .references(table("applications"), field("application_id")).onDeleteCascade()
                    ).getSQL());

            stmt.execute(create.createTable(table("memberships"))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("group_id", SQLDataType.BIGINT.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("user_id"), field("group_id")),
                            constraint().foreignKey(field("user_id"))
                                    .references(table("users"), field("user_id")).onDeleteCascade(),
                            constraint().foreignKey(field("group_id"))
                                    .references(table("groups"), field("group_id")).onDeleteCascade()
                    ).getSQL());

            stmt.execute(create.createTable(table("role_permissions"))
                    .column(field("role_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("permission_id", SQLDataType.BIGINT.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("role_id"), field("permission_id")),
                            constraint().foreignKey(field("role_id"))
                                    .references(table("roles"), field("role_id")).onDeleteCascade(),
                            constraint().foreignKey(field("permission_id"))
                                    .references(table("permissions"), field("permission_id")).onDeleteCascade()
                    ).getSQL());

            stmt.execute(create.createTable(table("assignments"))
                    .column(field("group_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("role_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("realm_id", SQLDataType.BIGINT.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("group_id"), field("role_id"), field("realm_id")),
                            constraint().foreignKey(field("group_id"))
                                    .references(table("groups"), field("group_id")).onDeleteCascade(),
                            constraint().foreignKey(field("role_id"))
                                    .references(table("roles"), field("role_id")).onDeleteCascade(),
                            constraint().foreignKey(field("realm_id"))
                                    .references(table("realms"), field("realm_id")).onDeleteCascade()
                    ).getSQL());

            stmt.execute(create.createTable(table("password_credentials"))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("password", SQLDataType.VARBINARY(256).nullable(false)))
                    .column(field("salt", SQLDataType.VARCHAR(16).nullable(false)))
                    .column(field("initial", SQLDataType.BOOLEAN.nullable(false)))
                    .column(field("created_at", SQLDataType.TIMESTAMP.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("user_id")),
                            constraint().foreignKey(field("user_id"))
                                    .references(table("users"), field("user_id")).onDeleteCascade()
                    ).getSQL());

            stmt.execute(create.createTable(table("otp_keys"))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("otp_key", SQLDataType.BINARY(20).nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("user_id")),
                            constraint().foreignKey(field("user_id"))
                                    .references(table("users"), field("user_id")).onDeleteCascade()
                    ).getSQL());

            stmt.execute(create.createTable(table("password_reset_challenges"))
                    .column(field("id", SQLDataType.BIGINT.identity(true)))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("code", SQLDataType.VARCHAR(64).nullable(false)))
                    .column(field("expires_at", SQLDataType.TIMESTAMP.nullable(false)))
                    .constraints(constraint().primaryKey(field("id")))
                    .getSQL());

            stmt.execute(create.createIndex(name("idx_pass_reset_challenges_01"))
                    .on(table("password_reset_challenges"), field("code"))
                    .getSQL());

            stmt.execute(create.createTable(table("actions"))
                    .column(field("action_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("name", SQLDataType.VARCHAR(100).nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("action_id")),
                            constraint().unique(field("name"))
                    ).getSQL());

            stmt.execute(create.createTable(table("user_actions"))
                    .column(field("user_action_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("action_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("actor", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("actor_ip", SQLDataType.VARCHAR(50).nullable(false)))
                    .column(field("options", SQLDataType.VARCHAR(4000)))
                    .column(field("created_at", SQLDataType.TIMESTAMP.nullable(false)))
                    .constraints(constraint().primaryKey(field("user_action_id")))
                    .getSQL());

            stmt.execute(create.createTable(table("oidc_providers"))
                    .column(field("oidc_provider_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("name", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("client_id", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("client_secret", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("scope", SQLDataType.VARCHAR(100)))
                    .column(field("response_type", SQLDataType.VARCHAR(100)))
                    .column(field("authorization_endpoint", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("token_endpoint", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("token_endpoint_auth_method", SQLDataType.VARCHAR(10).nullable(false)))
                    .column(field("name_lower", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("redirect_uri", SQLDataType.VARCHAR(255)))
                    .column(field("jwks_uri", SQLDataType.VARCHAR(512)))
                    .column(field("issuer", SQLDataType.VARCHAR(512)))
                    .column(field("pkce_enabled", SQLDataType.BOOLEAN.nullable(false).defaultValue(DSL.inline(false))))
                    .constraints(
                            constraint().primaryKey(field("oidc_provider_id")),
                            constraint().unique(field("name"))
                    ).getSQL());

            stmt.execute(create.createTable(table("certificate_credentials"))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("client_dn", SQLDataType.VARCHAR(150).nullable(false)))
                    .column(field("certificate", SQLDataType.VARBINARY(10000).nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("user_id")),
                            constraint().foreignKey(field("user_id"))
                                    .references(table("users"), field("user_id")).onDeleteCascade()
                    ).getSQL());

            stmt.execute(create.createTable(table("oidc_users"))
                    .column(field("oidc_provider_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("oidc_sub", SQLDataType.VARCHAR(255).nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("oidc_provider_id"), field("user_id")),
                            constraint().foreignKey(field("oidc_provider_id"))
                                    .references(table("oidc_providers"), field("oidc_provider_id")).onDeleteCascade(),
                            constraint().foreignKey(field("user_id"))
                                    .references(table("users"), field("user_id")).onDeleteCascade()
                    ).getSQL());

            stmt.execute(create.createTable(table("oidc_applications"))
                    .column(field("oidc_application_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("name", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("client_id", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("client_secret", SQLDataType.VARCHAR(512).nullable(false)))
                    .column(field("private_key", SQLDataType.VARBINARY(10000).nullable(false)))
                    .column(field("public_key", SQLDataType.VARBINARY(10000).nullable(false)))
                    .column(field("home_uri", SQLDataType.VARCHAR(2048)))
                    .column(field("callback_uri", SQLDataType.VARCHAR(2048)))
                    .column(field("description", SQLDataType.VARCHAR(255)))
                    .column(field("name_lower", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("backchannel_logout_uri", SQLDataType.VARCHAR(2048)))
                    .column(field("frontchannel_logout_uri", SQLDataType.VARCHAR(2048)))
                    .constraints(
                            constraint().primaryKey(field("oidc_application_id")),
                            constraint().unique(field("name"))
                    ).getSQL());

            stmt.execute(create.createTable(table("oidc_application_scopes"))
                    .column(field("oidc_application_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("permission_id", SQLDataType.BIGINT.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("oidc_application_id"), field("permission_id")),
                            constraint().foreignKey(field("oidc_application_id"))
                                    .references(table("oidc_applications"), field("oidc_application_id")).onDeleteCascade(),
                            constraint().foreignKey(field("permission_id"))
                                    .references(table("permissions"), field("permission_id")).onDeleteCascade()
                    ).getSQL());

            stmt.execute(create.createTable(table("oidc_application_grant_types"))
                    .column(field("oidc_application_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("grant_type", SQLDataType.VARCHAR(30).nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("oidc_application_id"), field("grant_type")),
                            constraint().foreignKey(field("oidc_application_id"))
                                    .references(table("oidc_applications"), field("oidc_application_id")).onDeleteCascade()
                    ).getSQL());

            stmt.execute(create.createTable(table("invitations"))
                    .column(field("invitation_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("email", SQLDataType.VARCHAR(100)))
                    .column(field("code", SQLDataType.VARCHAR(8)))
                    .column(field("invited_at", SQLDataType.TIMESTAMP.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("invitation_id")),
                            constraint().unique(field("code"))
                    ).getSQL());

            stmt.execute(create.createTable(table("group_invitations"))
                    .column(field("group_invitation_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("invitation_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("group_id", SQLDataType.BIGINT.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("group_invitation_id")),
                            constraint().foreignKey(field("invitation_id"))
                                    .references(table("invitations"), field("invitation_id")).onDeleteCascade(),
                            constraint().foreignKey(field("group_id"))
                                    .references(table("groups"), field("group_id")).onDeleteCascade()
                    ).getSQL());

            stmt.execute(create.createTable(table("oidc_invitations"))
                    .column(field("oidc_invitation_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("invitation_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("oidc_provider_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("oidc_payload", SQLDataType.VARCHAR(4000).nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("oidc_invitation_id")),
                            constraint().foreignKey(field("invitation_id"))
                                    .references(table("invitations"), field("invitation_id")).onDeleteCascade(),
                            constraint().foreignKey(field("oidc_provider_id"))
                                    .references(table("oidc_providers"), field("oidc_provider_id")).onDeleteCascade()
                    ).getSQL());

            stmt.execute(create.createTable(table("user_locks"))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("lock_level", SQLDataType.VARCHAR(10).nullable(false)))
                    .column(field("locked_at", SQLDataType.TIMESTAMP.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("user_id")),
                            constraint().foreignKey(field("user_id"))
                                    .references(table("users"), field("user_id")).onDeleteCascade()
                    ).getSQL());

            stmt.execute(create.createTable(table("user_sessions"))
                    .column(field("user_session_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("token", SQLDataType.VARCHAR(36).nullable(false)))
                    .column(field("remote_address", SQLDataType.VARCHAR(255)))
                    .column(field("user_agent", SQLDataType.VARCHAR(255)))
                    .column(field("created_at", SQLDataType.TIMESTAMP.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("user_session_id")),
                            constraint().unique(field("token")),
                            constraint().foreignKey(field("user_id"))
                                    .references(table("users"), field("user_id")).onDeleteCascade()
                    ).getSQL());

            stmt.execute(create.createTable(table("certs"))
                    .column(field("cert_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("serial", SQLDataType.BIGINT.nullable(false)))
                    .column(field("expires", SQLDataType.DATE.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("cert_id")),
                            constraint().unique(field("serial")),
                            constraint().foreignKey(field("user_id"))
                                    .references(table("users"), field("user_id"))
                    ).getSQL());

            stmt.execute(create.createTable(table("cert_devices"))
                    .column(field("cert_device_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("cert_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("device_token", SQLDataType.VARCHAR(36)))
                    .constraints(
                            constraint().primaryKey(field("cert_device_id")),
                            constraint().foreignKey(field("cert_id"))
                                    .references(table("certs"), field("cert_id")).onDeleteCascade()
                    ).getSQL());

            stmt.execute(create.createTable(table("user_profile_fields"))
                    .column(field("user_profile_field_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("name", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("json_name", SQLDataType.VARCHAR(100)))
                    .column(field("is_required", SQLDataType.BOOLEAN.nullable(false)))
                    .column(field("is_identity", SQLDataType.BOOLEAN.nullable(false)))
                    .column(field("regular_expression", SQLDataType.VARCHAR(255)))
                    .column(field("min_length", SQLDataType.SMALLINT))
                    .column(field("max_length", SQLDataType.SMALLINT))
                    .column(field("needs_verification", SQLDataType.BOOLEAN.nullable(false)))
                    .column(field("position", SQLDataType.SMALLINT.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("user_profile_field_id")),
                            constraint().unique(field("name")),
                            constraint().unique(field("json_name"))
                    ).getSQL());

            stmt.execute(create.createTable(table("user_profile_values"))
                    .column(field("user_profile_field_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field(name("value"), SQLDataType.VARCHAR(255).nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("user_profile_field_id"), field("user_id")),
                            constraint().foreignKey(field("user_profile_field_id"))
                                    .references(table("user_profile_fields"), field("user_profile_field_id")).onDeleteCascade(),
                            constraint().foreignKey(field("user_id"))
                                    .references(table("users"), field("user_id")).onDeleteCascade()
                    ).getSQL());

            stmt.execute(create.createTable(table("webauthn_credentials"))
                    .column(field("webauthn_credential_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("credential_id", SQLDataType.VARBINARY(1024).nullable(false)))
                    .column(field("credential_public_key", SQLDataType.VARBINARY(1024).nullable(false)))
                    .column(field("sign_count", SQLDataType.BIGINT.nullable(false).defaultValue(DSL.inline(0L))))
                    .column(field("transports", SQLDataType.VARCHAR(255)))
                    .column(field("attestation_format", SQLDataType.VARCHAR(32)))
                    .column(field("credential_name", SQLDataType.VARCHAR(100)))
                    .column(field("discoverable", SQLDataType.BOOLEAN.nullable(false).defaultValue(DSL.inline(true))))
                    .constraints(
                            constraint().primaryKey(field("webauthn_credential_id")),
                            constraint().unique(field("credential_id")),
                            constraint().foreignKey(field("user_id"))
                                    .references(table("users"), field("user_id")).onDeleteCascade()
                    ).getSQL());

            stmt.execute(create.createIndex(name("idx_webauthn_credentials_user"))
                    .on(table("webauthn_credentials"), field("user_id"))
                    .getSQL());

            stmt.execute(create.createTable(table("user_profile_verifications"))
                    .column(field("user_profile_field_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("code", SQLDataType.VARCHAR(255).nullable(false)))
                    .column(field("expires_at", SQLDataType.TIMESTAMP.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("user_profile_field_id"), field("user_id")),
                            constraint().foreignKey(field("user_profile_field_id"))
                                    .references(table("user_profile_fields"), field("user_profile_field_id")).onDeleteCascade(),
                            constraint().foreignKey(field("user_id"))
                                    .references(table("users"), field("user_id")).onDeleteCascade()
                    ).getSQL());
        }
    }

    private void seedActions(Connection connection) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO actions(action_id, name) VALUES (?, ?)")) {
            for (ActionType actionType : ActionType.values()) {
                stmt.setLong(1, actionType.getId());
                stmt.setString(2, actionType.getName());
                stmt.executeUpdate();
            }
        }
    }

    private void seedAdminData(Connection connection) throws SQLException {
        final String insUser = "INSERT INTO users(account, write_protected, account_lower) VALUES (?, ?, ?)";
        final String insPasswordCredential = "INSERT INTO password_credentials(user_id, password, salt, initial, created_at) VALUES (?, ?, ?, ?, ?)";
        final String insGroup = "INSERT INTO groups(name, description, write_protected, name_lower) VALUES (?, ?, ?, ?)";
        final String insRole = "INSERT INTO roles(name, description, write_protected, name_lower) VALUES (?, ?, ?, ?)";
        final String insPermission = "INSERT INTO permissions(name, description, write_protected, name_lower) VALUES (?, ?, ?, ?)";
        final String insRolePermission = "INSERT INTO role_permissions(role_id, permission_id) VALUES (?, ?)";
        final String insMembership = "INSERT INTO memberships(user_id, group_id) VALUES (?, ?)";
        final String insApplication = "INSERT INTO applications(name, description, pass_to, virtual_path, top_page, write_protected, name_lower) VALUES (?, ?, ?, ?, ?, ?, ?)";
        final String insRealm = "INSERT INTO realms(name, url, application_id, description, write_protected, name_lower) VALUES (?, ?, ?, ?, ?, ?)";
        final String insAssignment = "INSERT INTO assignments(group_id, role_id, realm_id) VALUES (?, ?, ?)";
        final String insUserProfileField = "INSERT INTO user_profile_fields(name, json_name, is_required, is_identity, regular_expression, min_length, max_length, needs_verification, position) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        final String insUserProfileValue = "INSERT INTO user_profile_values(user_profile_field_id, user_id, \"value\") VALUES (?, ?, ?)";

        try (PreparedStatement stmtInsUser = connection.prepareStatement(insUser, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtInsPasswdCred = connection.prepareStatement(insPasswordCredential);
             PreparedStatement stmtInsPermission = connection.prepareStatement(insPermission, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtInsRole = connection.prepareStatement(insRole, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtInsRolePermission = connection.prepareStatement(insRolePermission);
             PreparedStatement stmtInsMembership = connection.prepareStatement(insMembership);
             PreparedStatement stmtInsGroup = connection.prepareStatement(insGroup, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtInsRealm = connection.prepareStatement(insRealm, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtInsApplication = connection.prepareStatement(insApplication, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtInsAssignment = connection.prepareStatement(insAssignment);
             PreparedStatement stmtInsUserProfileField = connection.prepareStatement(insUserProfileField, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtInsUserProfileValue = connection.prepareStatement(insUserProfileValue)) {

            stmtInsUserProfileField.setString(1, "Email");
            stmtInsUserProfileField.setString(2, "email");
            stmtInsUserProfileField.setBoolean(3, true);
            stmtInsUserProfileField.setBoolean(4, true);
            stmtInsUserProfileField.setString(5, "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
            stmtInsUserProfileField.setNull(6, java.sql.Types.SMALLINT);
            stmtInsUserProfileField.setInt(7, 255);
            stmtInsUserProfileField.setBoolean(8, true);
            stmtInsUserProfileField.setInt(9, 1);
            stmtInsUserProfileField.executeUpdate();
            Long emailFieldId = fetchGeneratedKey(stmtInsUserProfileField);

            stmtInsUserProfileField.setString(1, "Name");
            stmtInsUserProfileField.setString(2, "name");
            stmtInsUserProfileField.setBoolean(3, true);
            stmtInsUserProfileField.setBoolean(4, false);
            stmtInsUserProfileField.setString(5, null);
            stmtInsUserProfileField.setNull(6, java.sql.Types.SMALLINT);
            stmtInsUserProfileField.setInt(7, 255);
            stmtInsUserProfileField.setBoolean(8, false);
            stmtInsUserProfileField.setInt(9, 2);
            stmtInsUserProfileField.executeUpdate();
            Long nameFieldId = fetchGeneratedKey(stmtInsUserProfileField);

            stmtInsUser.setString(1, "admin");
            stmtInsUser.setBoolean(2, true);
            stmtInsUser.setString(3, "admin");
            stmtInsUser.executeUpdate();
            Long userId = fetchGeneratedKey(stmtInsUser);

            stmtInsUserProfileValue.setLong(1, emailFieldId);
            stmtInsUserProfileValue.setLong(2, userId);
            stmtInsUserProfileValue.setString(3, "admin@example.com");
            stmtInsUserProfileValue.executeUpdate();

            stmtInsUserProfileValue.setLong(1, nameFieldId);
            stmtInsUserProfileValue.setLong(2, userId);
            stmtInsUserProfileValue.setString(3, "Admin User");
            stmtInsUserProfileValue.executeUpdate();

            SecureRandom random = new SecureRandom();
            String adminPassword = Env.getString("ADMIN_PASSWORD", null);
            if (adminPassword == null) {
                adminPassword = net.unit8.bouncr.util.RandomUtils.generateRandomString(20, random);
            }
            String adminSalt = net.unit8.bouncr.util.RandomUtils.generateRandomString(16, random);
            int iterations = Env.getInt("pbkdf2.iterations", 10000);

            boolean isInitial = Env.getString("ADMIN_PASSWORD", null) == null;
            stmtInsPasswdCred.setLong(1, userId);
            stmtInsPasswdCred.setBytes(2, PasswordUtils.pbkdf2(adminPassword, adminSalt, iterations));
            stmtInsPasswdCred.setString(3, adminSalt);
            stmtInsPasswdCred.setBoolean(4, isInitial);
            stmtInsPasswdCred.setTimestamp(5, Timestamp.from(Instant.now()));
            stmtInsPasswdCred.executeUpdate();

            if (isInitial) {
                LOG.warn("Initial admin password: {} (change immediately after first login)", adminPassword);
            }

            stmtInsGroup.setString(1, "BOUNCR_ADMIN");
            stmtInsGroup.setString(2, "Bouncr administrators");
            stmtInsGroup.setBoolean(3, true);
            stmtInsGroup.setString(4, "bouncr_admin");
            stmtInsGroup.executeUpdate();
            Long adminGroupId = fetchGeneratedKey(stmtInsGroup);

            stmtInsGroup.setString(1, "BOUNCR_USER");
            stmtInsGroup.setString(2, "Bouncr users");
            stmtInsGroup.setBoolean(3, true);
            stmtInsGroup.setString(4, "bouncr_user");
            stmtInsGroup.executeUpdate();
            Long userGroupId = fetchGeneratedKey(stmtInsGroup);

            stmtInsMembership.setLong(1, userId);
            stmtInsMembership.setLong(2, adminGroupId);
            stmtInsMembership.executeUpdate();

            stmtInsMembership.setLong(1, userId);
            stmtInsMembership.setLong(2, userGroupId);
            stmtInsMembership.executeUpdate();

            stmtInsApplication.setString(1, "BOUNCR");
            stmtInsApplication.setString(2, "Bouncr API");
            stmtInsApplication.setString(3, Env.getString("API_BACKEND_URL", "http://api:3005/bouncr/api"));
            stmtInsApplication.setString(4, "/bouncr/api");
            stmtInsApplication.setString(5, "/bouncr/api");
            stmtInsApplication.setBoolean(6, true);
            stmtInsApplication.setString(7, "bouncr");
            stmtInsApplication.executeUpdate();
            Long applicationId = fetchGeneratedKey(stmtInsApplication);

            stmtInsRealm.setString(1, "BOUNCR");
            stmtInsRealm.setString(2, ".*");
            stmtInsRealm.setLong(3, applicationId);
            stmtInsRealm.setString(4, "Bouncr Application Realm");
            stmtInsRealm.setBoolean(5, true);
            stmtInsRealm.setString(6, "bouncr");
            stmtInsRealm.executeUpdate();
            Long bouncrRealmId = fetchGeneratedKey(stmtInsRealm);

            stmtInsRole.setString(1, "BOUNCR_ADMIN");
            stmtInsRole.setString(2, "Bouncer administrations");
            stmtInsRole.setBoolean(3, true);
            stmtInsRole.setString(4, "bouncr_admin");
            stmtInsRole.executeUpdate();
            Long adminRoleId = fetchGeneratedKey(stmtInsRole);

            Arrays.asList(ADMIN_PERMISSIONS).forEach(perm -> {
                try {
                    Long permissionId = createPermission(perm, stmtInsPermission);
                    stmtInsRolePermission.setLong(1, adminRoleId);
                    stmtInsRolePermission.setLong(2, permissionId);
                    stmtInsRolePermission.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            Arrays.asList(OTHER_PERMISSIONS).forEach(perm -> {
                try {
                    createPermission(perm, stmtInsPermission);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            stmtInsRole.setString(1, "BOUNCR_USER");
            stmtInsRole.setString(2, "Bouncr users");
            stmtInsRole.setBoolean(3, true);
            stmtInsRole.setString(4, "bouncr_user");
            stmtInsRole.executeUpdate();
            Long myRoleId = fetchGeneratedKey(stmtInsRole);

            Long myReadPermissionId = createPermission("my:read", stmtInsPermission);
            Long myUpdatePermissionId = createPermission("my:update", stmtInsPermission);
            Long myDeletePermissionId = createPermission("my:delete", stmtInsPermission);

            stmtInsRolePermission.setLong(1, myRoleId);
            stmtInsRolePermission.setLong(2, myReadPermissionId);
            stmtInsRolePermission.executeUpdate();

            stmtInsRolePermission.setLong(1, myRoleId);
            stmtInsRolePermission.setLong(2, myUpdatePermissionId);
            stmtInsRolePermission.executeUpdate();

            stmtInsRolePermission.setLong(1, myRoleId);
            stmtInsRolePermission.setLong(2, myDeletePermissionId);
            stmtInsRolePermission.executeUpdate();

            // OAuth2 endpoints application
            stmtInsApplication.setString(1, "BOUNCR_OAUTH2");
            stmtInsApplication.setString(2, "OAuth2 endpoints");
            stmtInsApplication.setString(3, Env.getString("API_BACKEND_URL_OAUTH2", "http://api:3005/oauth2"));
            stmtInsApplication.setString(4, "/oauth2");
            stmtInsApplication.setString(5, "/oauth2");
            stmtInsApplication.setBoolean(6, true);
            stmtInsApplication.setString(7, "bouncr_oauth2");
            stmtInsApplication.executeUpdate();
            Long oauth2ApplicationId = fetchGeneratedKey(stmtInsApplication);

            stmtInsRealm.setString(1, "BOUNCR_OAUTH2");
            stmtInsRealm.setString(2, ".*");
            stmtInsRealm.setLong(3, oauth2ApplicationId);
            stmtInsRealm.setString(4, "OAuth2 realm");
            stmtInsRealm.setBoolean(5, true);
            stmtInsRealm.setString(6, "bouncr_oauth2");
            stmtInsRealm.executeUpdate();
            fetchGeneratedKey(stmtInsRealm); // realm ID not needed (no assignments for OAuth2 realm)

            stmtInsAssignment.setLong(1, adminGroupId);
            stmtInsAssignment.setLong(2, adminRoleId);
            stmtInsAssignment.setLong(3, bouncrRealmId);
            stmtInsAssignment.executeUpdate();

            stmtInsAssignment.setLong(1, userGroupId);
            stmtInsAssignment.setLong(2, myRoleId);
            stmtInsAssignment.setLong(3, bouncrRealmId);
            stmtInsAssignment.executeUpdate();
        }
    }

    private Long createPermission(String permission, PreparedStatement stmt) throws SQLException {
        stmt.setString(1, permission);
        stmt.setString(2, permission);
        stmt.setBoolean(3, true);
        stmt.setString(4, permission.toLowerCase(Locale.US));
        stmt.executeUpdate();
        return fetchGeneratedKey(stmt);
    }

    private Long fetchGeneratedKey(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.getGeneratedKeys()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            throw new SQLException("Generated key is not found.");
        }
    }
}
