package db.migration;

import net.unit8.bouncr.util.PasswordUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.*;
import java.util.Arrays;

import static org.jooq.impl.DSL.*;

public class V23__InsertAdminUser extends BaseJavaMigration {
    private static final String[] ADMIN_PERMISSIONS = new String[]{
            "LIST_ANY_USERS", "CREATE_ANY_USER", "MODIFY_ANY_USER", "DELETE_ANY_USER",
            "LOCK_ANY_USER", "UNLOCK_ANY_USER",
            "LIST_ANY_GROUPS", "CREATE_GROUP", "MODIFY_ANY_GROUP", "DELETE_ANY_GROUP",
            "CREATE_MEMBERSHIP", "DELETE_MEMBERSHIP",
            "LIST_ANY_APPLICATIONS", "CREATE_APPLICATION", "MODIFY_ANY_APPLICATION", "DELETE_ANY_APPLICATION",
            "LIST_ANY_REALMS", "CREATE_REALM", "MODIFY_ANY_REALM", "DELETE_ANY_REALM",
            "LIST_ANY_ROLES", "CREATE_ROLE", "MODIFY_ANY_ROLE", "DELETE_ANY_ROLE",
            "LIST_ANY_PERMISSIONS", "CREATE_PERMISSION", "MODIFY_ANY_PERMISSION", "DELETE_ANY_PERMISSION",
            "LIST_OIDC_APPLICATIONS", "CREATE_OIDC_APPLICATION", "MODIFY_OIDC_APPLICATION", "DELETE_OIDC_APPLICATION",
            "LIST_OIDC_PROVIDERS", "CREATE_OIDC_PROVIDER", "MODIFY_OIDC_PROVIDER", "DELETE_OIDC_PROVIDER",
            "LIST_USER_PROFILE_FIELDS", "CREATE_USER_PROFILE_FIELD", "MODIFY_USER_PROFILE_FIELD", "DELETE_USER_PROFILE_FIELD",
            "CREATE_INVITATION"
    };

    private static final String[] OTHER_PERMISSIONS = new String[]{
            "LIST_USERS", "MODIFY_USER", "DELETE_USER",
            "LOCK_USER", "UNLOCK_USER",
            "LIST_GROUPS", "MODIFY_GROUP", "DELETE_GROUP",
            "LIST_APPLICATIONS", "MODIFY_APPLICATION", "DELETE_APPLICATION",
            "LIST_REALMS", "MODIFY_REALM", "DELETE_REALM",
            "LIST_ROLES", "MODIFY_ROLE", "DELETE_ROLE",
            "LIST_PERMISSIONS", "MODIFY_PERMISSION", "DELETE_PERMISSION",
    };
    private Long fetchGeneratedKey(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.getGeneratedKeys()) {
            while(rs.next()) {
                return rs.getLong(1);
            }
            throw new SQLException("Generated key is not found.");
        }
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        DSLContext create = DSL.using(connection);
        final String INS_USER = create
                .insertInto(table("users"))
                .columns(
                        field("account"),
                        field("write_protected"))
                .values(param(), param(SQLDataType.BOOLEAN))
                .getSQL();

        final String INS_PASSWD_CRED = create
                .insertInto(table("password_credentials"))
                .columns(
                        field("user_id"),
                        field("password"),
                        field("salt"),
                        field("initial"),
                        field("created_at"))
                .values(param(SQLDataType.BIGINT),
                        param(SQLDataType.BINARY),
                        param(),
                        param(SQLDataType.BOOLEAN),
                        param(Date.class))
                .getSQL();

        final String INS_GROUP = create
                .insertInto(table("groups"))
                .columns(
                        field("name"),
                        field("description"),
                        field("write_protected")
                )
                .values("?", "?", "?")
                .getSQL();

        final String INS_ROLE = create
                .insertInto(table("roles"))
                .columns(
                        field("name"),
                        field("description"),
                        field("write_protected")
                )
                .values("?", "?", "?")
                .getSQL();

        final String INS_PERMISSION = create
                .insertInto(table("permissions"))
                .columns(
                        field("name"),
                        field("description"),
                        field("write_protected")
                )
                .values("?", "?", "?")
                .getSQL();

        final String INS_ROLE_PERMISSION = create
                .insertInto(table("role_permissions"))
                .columns(
                        field("role_id"),
                        field("permission_id")
                )
                .values("?", "?")
                .getSQL();

        final String INS_MEMBERSHIP = create
                .insertInto(table("memberships"))
                .columns(
                        field("user_id"),
                        field("group_id")
                )
                .values("?", "?")
                .getSQL();

        final String INS_APPLICATION = create
                .insertInto(table("applications"))
                .columns(
                        field("name"),
                        field("description"),
                        field("pass_to"),
                        field("virtual_path"),
                        field("top_page"),
                        field("write_protected"))
                .values("?", "?", "?", "?", "?", "?")
                .getSQL();

        final String INS_REALM = create
                .insertInto(table("realms"))
                .columns(
                        field("name"),
                        field("url"),
                        field("application_id"),
                        field("description"),
                        field("write_protected")
                )
                .values("?", "?", "?", "?", "?")
                .getSQL();

        final String INS_ASSIGNMENT = create
                .insertInto(table("assignments"))
                .columns(
                        field("group_id"),
                        field("role_id"),
                        field("realm_id")
                )
                .values("?", "?", "?")
                .getSQL();
        final String INS_USER_PROFILE_FIELD = create
                .insertInto(table("user_profile_fields"))
                .columns(
                        field("name"),
                        field("json_name"),
                        field("is_required"),
                        field("is_identity"),
                        field("regular_expression"),
                        field("min_length"),
                        field("max_length"),
                        field("needs_verification"),
                        field("position")
                ).values("?", "?", "?", "?", "?", "?","?", "?", "?")
                .getSQL();
        final String INS_USER_PROFILE_VALUE = create
                .insertInto(table("user_profile_values"))
                .columns(
                        field("user_profile_field_id"),
                        field("user_id"),
                        field("value")
                ).values("?", "?", "?")
                .getSQL();
        try (PreparedStatement stmtInsUser = connection.prepareStatement(INS_USER, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtInsPasswdCred = connection.prepareStatement(INS_PASSWD_CRED);
             PreparedStatement stmtInsPermission = connection.prepareStatement(INS_PERMISSION, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtInsRole = connection.prepareStatement(INS_ROLE, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtInsRolePermission = connection.prepareStatement(INS_ROLE_PERMISSION);
             PreparedStatement stmtInsMembership = connection.prepareStatement(INS_MEMBERSHIP, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtInsGroup = connection.prepareStatement(INS_GROUP, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtInsRealm = connection.prepareStatement(INS_REALM, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtInsApplication = connection.prepareStatement(INS_APPLICATION, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtInsAssignment = connection.prepareStatement(INS_ASSIGNMENT);
             PreparedStatement stmtInsUserProfileField = connection.prepareStatement(INS_USER_PROFILE_FIELD, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtInsUserProfileValue = connection.prepareStatement(INS_USER_PROFILE_VALUE, Statement.RETURN_GENERATED_KEYS)
             ) {
            // Email Field
            stmtInsUserProfileField.setString(1, "Email");
            stmtInsUserProfileField.setString(2, "email");
            stmtInsUserProfileField.setBoolean(3, true);
            stmtInsUserProfileField.setBoolean(4, true);
            stmtInsUserProfileField.setString(5, "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
            stmtInsUserProfileField.setNull(6, 0);
            stmtInsUserProfileField.setInt(7, 255);
            stmtInsUserProfileField.setBoolean(8, true);
            stmtInsUserProfileField.setInt(9, 1);
            stmtInsUserProfileField.executeUpdate();
            Long emailFieldId = fetchGeneratedKey(stmtInsUserProfileField);

            // Name Field
            stmtInsUserProfileField.setString(1, "Name");
            stmtInsUserProfileField.setString(2, "name");
            stmtInsUserProfileField.setBoolean(3, true);
            stmtInsUserProfileField.setBoolean(4, false);
            stmtInsUserProfileField.setString(5, null);
            stmtInsUserProfileField.setNull(6, 0);
            stmtInsUserProfileField.setInt(7, 255);
            stmtInsUserProfileField.setBoolean(8, false);
            stmtInsUserProfileField.setInt(9, 2);
            stmtInsUserProfileField.executeUpdate();
            Long nameFieldId = fetchGeneratedKey(stmtInsUserProfileField);

            stmtInsUser.setString(1, "admin");
            stmtInsUser.setBoolean(2, true);
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

            stmtInsPasswdCred.setLong(1, userId);
            stmtInsPasswdCred.setBytes(2, PasswordUtils.pbkdf2("password", "0123456789012345", 100));
            stmtInsPasswdCred.setString(3, "0123456789012345");
            stmtInsPasswdCred.setBoolean(4, false);
            stmtInsPasswdCred.setDate(5, new Date(System.currentTimeMillis()));
            stmtInsPasswdCred.executeUpdate();

            stmtInsGroup.setString(1, "BOUNCR_ADMIN");
            stmtInsGroup.setString(2, "Bouncr administrators");
            stmtInsGroup.setBoolean(3, true);
            stmtInsGroup.executeUpdate();
            Long adminGroupId = fetchGeneratedKey(stmtInsGroup);

            stmtInsGroup.setString(1, "BOUNCR_USER");
            stmtInsGroup.setString(2, "Bouncr users");
            stmtInsGroup.setBoolean(3, true);
            stmtInsGroup.executeUpdate();
            Long userGroupId = fetchGeneratedKey(stmtInsGroup);

            stmtInsMembership.setLong(1, userId);
            stmtInsMembership.setLong(2, adminGroupId);
            stmtInsMembership.executeUpdate();

            stmtInsMembership.setLong(1, userId);
            stmtInsMembership.setLong(2, userGroupId);
            stmtInsMembership.executeUpdate();

            stmtInsApplication.setString(1, "BOUNCR");
            stmtInsApplication.setString(2, "Bouncer application");
            stmtInsApplication.setString(3, "http://api:3005/bouncr/api");
            stmtInsApplication.setString(4, "/bouncr/api");
            stmtInsApplication.setString(5, "/bouncr/api");
            stmtInsApplication.setBoolean(6, true);
            stmtInsApplication.executeUpdate();
            Long applicationId = fetchGeneratedKey(stmtInsApplication);

            stmtInsRealm.setString(1, "BOUNCR");
            stmtInsRealm.setString(2, ".*");
            stmtInsRealm.setLong(3, applicationId);
            stmtInsRealm.setString(4, "Bouncr Application Realm");
            stmtInsRealm.setBoolean(5, true);
            stmtInsRealm.executeUpdate();
            Long bouncrRealmId = fetchGeneratedKey(stmtInsRealm);

            stmtInsRole.setString(1, "BOUNCR_ADMIN");
            stmtInsRole.setString(2, "Bouncer administrations");
            stmtInsRole.setBoolean(3, true);
            stmtInsRole.executeUpdate();
            Long adminRoleId = fetchGeneratedKey(stmtInsRole);

            Arrays.asList(ADMIN_PERMISSIONS).forEach(perm -> {
                try {
                    stmtInsPermission.setString(1, perm);
                    stmtInsPermission.setString(2, "");
                    stmtInsPermission.setBoolean(3, true);
                    stmtInsPermission.executeUpdate();
                    Long permissionId = fetchGeneratedKey(stmtInsPermission);

                    stmtInsRolePermission.setLong(1, adminRoleId);
                    stmtInsRolePermission.setLong(2, permissionId);
                    stmtInsRolePermission.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            Arrays.asList(OTHER_PERMISSIONS).forEach(perm -> {
                try {
                    stmtInsPermission.setString(1, perm);
                    stmtInsPermission.setString(2, perm);
                    stmtInsPermission.setBoolean(3, true);
                    stmtInsPermission.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            stmtInsRole.setString(1, "BOUNCR_USER");
            stmtInsRole.setString(2, "Bouncr users");
            stmtInsRole.setBoolean(3, true);
            stmtInsRole.executeUpdate();
            Long myRoleId = fetchGeneratedKey(stmtInsRole);
            stmtInsPermission.setString(1, "READ_USER");
            stmtInsPermission.setString(2, "READ_USER");
            stmtInsPermission.setBoolean(3, true);
            stmtInsPermission.executeUpdate();
            Long readUserPermissionId = fetchGeneratedKey(stmtInsPermission);
            stmtInsRolePermission.setLong(1, myRoleId);
            stmtInsRolePermission.setLong(2, readUserPermissionId);
            stmtInsRolePermission.executeUpdate();

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
}
