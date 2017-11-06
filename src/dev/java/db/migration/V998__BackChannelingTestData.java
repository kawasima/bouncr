package db.migration;

import net.unit8.bouncr.util.PasswordUtils;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.sql.*;
import java.util.Arrays;

import static org.jooq.impl.DSL.*;

public class V998__BackChannelingTestData implements JdbcMigration {
    private static final String[] ADMIN_PERMISSIONS = new String[]{
            "create-board", "read-board", "modify-board",
            "read-any-thread", "read-thread", "write-any-thread", "write-thread",
            "delete-any-comment", "delete-comment"
    };

    private static final String[] OTHER_PERMISSIONS = new String[]{
            "read-board",
            "read-thread", "write-thread",
            "delete-comment"
    };

    private Long fetchGeneratedKey(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.getGeneratedKeys()) {
            while (rs.next()) {
                return rs.getLong(1);
            }
            throw new SQLException("Generated key is not found.");
        }
    }

    @Override
    public void migrate(Connection connection) throws Exception {
        DSLContext create = DSL.using(connection);
        final String INS_USER = create
                .insertInto(table("users"))
                .columns(
                        field("account"),
                        field("name"),
                        field("email"),
                        field("write_protected"))
                .values(param(), param(), param(), param())
                .getSQL();

        final String INS_PASSWD_CRED = create
                .insertInto(table("password_credentials"))
                .columns(
                        field("user_id"),
                        field("password"),
                        field("salt"),
                        field("initial"),
                        field("created_at"))
                .values(param(), param(), param(), param(), param(Date.class))
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

        final String SELECT_GROUP = create
                .select(field("group_id"))
                .from(table("groups"))
                .where(field("name").eq("?"))
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

        final String SELECT_PERMISSION = create
                .select(field("permission_id"))
                .from(table("permissions"))
                .where(field("name").eq("?"))
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

        try (PreparedStatement stmtInsUser = connection.prepareStatement(INS_USER, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtInsPasswdCred = connection.prepareStatement(INS_PASSWD_CRED);
             PreparedStatement stmtInsPermission = connection.prepareStatement(INS_PERMISSION, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtSelPermission = connection.prepareStatement(SELECT_PERMISSION);
             PreparedStatement stmtInsRole = connection.prepareStatement(INS_ROLE, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtInsRolePermission = connection.prepareStatement(INS_ROLE_PERMISSION);
             PreparedStatement stmtInsMembership = connection.prepareStatement(INS_MEMBERSHIP, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtInsGroup = connection.prepareStatement(INS_GROUP, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtInsRealm = connection.prepareStatement(INS_REALM, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtInsApplication = connection.prepareStatement(INS_APPLICATION, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtInsAssignment = connection.prepareStatement(INS_ASSIGNMENT);
             PreparedStatement stmtSelGroup = connection.prepareStatement(SELECT_GROUP);
        ) {
            stmtInsUser.setString(1, "user1");
            stmtInsUser.setString(2, "user1");
            stmtInsUser.setString(3, "user1@example.com");
            stmtInsUser.setBoolean(4, true);
            stmtInsUser.executeUpdate();
            Long user1Id = fetchGeneratedKey(stmtInsUser);

            stmtInsUser.setString(1, "user2");
            stmtInsUser.setString(2, "user2");
            stmtInsUser.setString(3, "user2@example.com");
            stmtInsUser.setBoolean(4, true);
            stmtInsUser.executeUpdate();
            Long user2Id = fetchGeneratedKey(stmtInsUser);

            stmtInsUser.setString(1, "user3");
            stmtInsUser.setString(2, "user3");
            stmtInsUser.setString(3, "user3@example.com");
            stmtInsUser.setBoolean(4, true);
            stmtInsUser.executeUpdate();
            Long user3Id = fetchGeneratedKey(stmtInsUser);

            Arrays.asList(user1Id, user2Id, user3Id).forEach(id -> {
                try {
                    stmtInsPasswdCred.setLong(1, id);
                    stmtInsPasswdCred.setBytes(2, PasswordUtils.pbkdf2("password", "0123456789012345", 100));
                    stmtInsPasswdCred.setString(3, "0123456789012345");
                    stmtInsPasswdCred.setBoolean(4, false);
                    stmtInsPasswdCred.setDate(5, new Date(System.currentTimeMillis() / 1000));
                    stmtInsPasswdCred.executeUpdate();
                } catch(SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            stmtInsGroup.setString(1, "BC_DEFAULT");
            stmtInsGroup.setString(2, "Default group for BackChanneling");
            stmtInsGroup.setBoolean(3, false);
            stmtInsGroup.executeUpdate();
            Long defaultGroupId = fetchGeneratedKey(stmtInsGroup);

            stmtInsGroup.setString(1, "BC_BOARD1");
            stmtInsGroup.setString(2, "A group for default board1");
            stmtInsGroup.setBoolean(3, false);
            stmtInsGroup.executeUpdate();
            Long board1GroupId = fetchGeneratedKey(stmtInsGroup);

            stmtInsMembership.setLong(1, user1Id);
            stmtInsMembership.setLong(2, defaultGroupId);
            stmtInsMembership.executeUpdate();

            stmtInsMembership.setLong(1, user1Id);
            stmtInsMembership.setLong(2, board1GroupId);
            stmtInsMembership.executeUpdate();

            stmtInsMembership.setLong(1, user2Id);
            stmtInsMembership.setLong(2, defaultGroupId);
            stmtInsMembership.executeUpdate();

            stmtSelGroup.setString(1, "BOUNCR_USER");
            try (ResultSet rs = stmtSelGroup.executeQuery()) {
                if (rs.next()) {
                    final Long bouncrUserGroup = rs.getLong(1);

                    Arrays.asList(user1Id, user2Id, user3Id).forEach(id -> {
                        try {
                            stmtInsMembership.setLong(1, id);
                            stmtInsMembership.setLong(2, bouncrUserGroup);
                            stmtInsMembership.executeUpdate();
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }

            // --------------------------------------
            // Application & Realms
            // --------------------------------------

            stmtInsApplication.setString(1, "BackChanneling");
            stmtInsApplication.setString(2, "BackChanneling");
            stmtInsApplication.setString(3, "http://localhost:3009/bc/");
            stmtInsApplication.setString(4, "/bc");
            stmtInsApplication.setString(5, "http://localhost:3000/bc/");
            stmtInsApplication.setBoolean(6, false);
            stmtInsApplication.executeUpdate();
            Long applicationId = fetchGeneratedKey(stmtInsApplication);

            stmtInsRealm.setString(1, "BC_OPEN");
            stmtInsRealm.setString(2, "(?!api/board/).*");
            stmtInsRealm.setLong(3, applicationId);
            stmtInsRealm.setString(4, "BackChannelingOpen Realm");
            stmtInsRealm.setBoolean(5, false);
            stmtInsRealm.executeUpdate();
            Long bcOpenRealmId = fetchGeneratedKey(stmtInsRealm);

            stmtInsRealm.setString(1, "BC_DEFAULT");
            stmtInsRealm.setString(2, "api/board/default($|/.*)");
            stmtInsRealm.setLong(3, applicationId);
            stmtInsRealm.setString(4, "BackChanneling default board");
            stmtInsRealm.setBoolean(5, false);
            stmtInsRealm.executeUpdate();
            Long bcDefaultRealmId = fetchGeneratedKey(stmtInsRealm);

            stmtInsRealm.setString(1, "BC_BOARD1");
            stmtInsRealm.setString(2, "api/board/board1($|/.*)");
            stmtInsRealm.setLong(3, applicationId);
            stmtInsRealm.setString(4, "BackChanneling board1");
            stmtInsRealm.setBoolean(5, false);
            stmtInsRealm.executeUpdate();
            Long bcBoard1RealmId = fetchGeneratedKey(stmtInsRealm);

            // --------------------------------------
            // Role
            // --------------------------------------
            stmtInsRole.setString(1, "BC_ADMIN");
            stmtInsRole.setString(2, "BackChanneling administrations");
            stmtInsRole.setBoolean(3, false);
            stmtInsRole.executeUpdate();
            Long adminRoleId = fetchGeneratedKey(stmtInsRole);

            stmtInsRole.setString(1, "BC_USER");
            stmtInsRole.setString(2, "BackChanneling users");
            stmtInsRole.setBoolean(3, false);
            stmtInsRole.executeUpdate();
            Long userRoleId = fetchGeneratedKey(stmtInsRole);


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
                    Long permissionId = 0L;
                    if (Arrays.asList(ADMIN_PERMISSIONS).contains(perm)) {
                        stmtSelPermission.setString(1, perm);
                        try (ResultSet rs = stmtSelPermission.executeQuery()) {
                            // the name column of the permission table is unique, only one data can not be get
                            if (rs.next()) {
                                permissionId = rs.getLong("permission_id");
                            }
                        }
                    } else {
                        stmtInsPermission.setString(1, perm);
                        stmtInsPermission.setString(2, "");
                        stmtInsPermission.setBoolean(3, true);
                        stmtInsPermission.executeUpdate();
                        permissionId = fetchGeneratedKey(stmtInsPermission);
                    }

                    stmtInsRolePermission.setLong(1, userRoleId);
                    stmtInsRolePermission.setLong(2, permissionId);
                    stmtInsRolePermission.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });


            // All users have the BC_USER role at other board.
            stmtInsAssignment.setLong(1, defaultGroupId);
            stmtInsAssignment.setLong(2, userRoleId);
            stmtInsAssignment.setLong(3, bcOpenRealmId);
            stmtInsAssignment.executeUpdate();

            // All users have the BC_USER role at default board.
            stmtInsAssignment.setLong(1, defaultGroupId);
            stmtInsAssignment.setLong(2, userRoleId);
            stmtInsAssignment.setLong(3, bcDefaultRealmId);
            stmtInsAssignment.executeUpdate();

            // The users in the Board1 group have the BC_USER role at board1.
            stmtInsAssignment.setLong(1, board1GroupId);
            stmtInsAssignment.setLong(2, userRoleId);
            stmtInsAssignment.setLong(3, bcBoard1RealmId);
            stmtInsAssignment.executeUpdate();
        }
    }
}
