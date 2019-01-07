package db.migration;

import net.unit8.bouncr.util.PasswordUtils;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.sql.*;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jooq.impl.DSL.*;

public class V997__RotomTestData implements JdbcMigration {
    private static final String[] READER_PERMISSIONS = new String[]{
            "page:read"
    };

    private static final String[] CURATOR_PERMISSIONS = new String[]{
            "page:create", "page:edit", "page:delete"
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
        /*
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
            stmtInsUser.setString(1, "reader");
            stmtInsUser.setString(2, "reader");
            stmtInsUser.setString(3, "reader@example.com");
            stmtInsUser.setBoolean(4, true);
            stmtInsUser.executeUpdate();
            Long readerId = fetchGeneratedKey(stmtInsUser);

            stmtInsUser.setString(1, "curator");
            stmtInsUser.setString(2, "curator");
            stmtInsUser.setString(3, "curator@example.com");
            stmtInsUser.setBoolean(4, true);
            stmtInsUser.executeUpdate();
            Long curatorId = fetchGeneratedKey(stmtInsUser);

            Arrays.asList(readerId, curatorId).forEach(id -> {
                try {
                    stmtInsPasswdCred.setLong(1, id);
                    stmtInsPasswdCred.setBytes(2, PasswordUtils.pbkdf2("password", "0123456789012345", 100));
                    stmtInsPasswdCred.setString(3, "0123456789012345");
                    stmtInsPasswdCred.setBoolean(4, false);
                    stmtInsPasswdCred.setDate(5, new Date(System.currentTimeMillis()));
                    stmtInsPasswdCred.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            stmtInsGroup.setString(1, "ROTOM_READER");
            stmtInsGroup.setString(2, "Reader group for Rotom");
            stmtInsGroup.setBoolean(3, false);
            stmtInsGroup.executeUpdate();
            Long readerGroupId = fetchGeneratedKey(stmtInsGroup);

            stmtInsGroup.setString(1, "ROTOM_CURATOR");
            stmtInsGroup.setString(2, "Curator group for Rotom");
            stmtInsGroup.setBoolean(3, false);
            stmtInsGroup.executeUpdate();
            Long curatorGroupId = fetchGeneratedKey(stmtInsGroup);

            stmtInsMembership.setLong(1, readerId);
            stmtInsMembership.setLong(2, readerGroupId);
            stmtInsMembership.executeUpdate();

            stmtInsMembership.setLong(1, curatorId);
            stmtInsMembership.setLong(2, readerGroupId);
            stmtInsMembership.executeUpdate();

            stmtInsMembership.setLong(1, curatorId);
            stmtInsMembership.setLong(2, curatorGroupId);
            stmtInsMembership.executeUpdate();

            stmtSelGroup.setString(1, "BOUNCR_USER");
            try (ResultSet rs = stmtSelGroup.executeQuery()) {
                if (rs.next()) {
                    final Long bouncrUserGroup = rs.getLong(1);

                    Arrays.asList(readerId, curatorId).forEach(id -> {
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

            stmtInsApplication.setString(1, "Rotom");
            stmtInsApplication.setString(2, "Rotom");
            stmtInsApplication.setString(3, "http://localhost:3010/wiki/");
            stmtInsApplication.setString(4, "/wiki");
            stmtInsApplication.setString(5, "http://localhost:3000/wiki/");
            stmtInsApplication.setBoolean(6, false);
            stmtInsApplication.executeUpdate();
            Long applicationId = fetchGeneratedKey(stmtInsApplication);

            stmtInsRealm.setString(1, "ROTOM_OPEN");
            stmtInsRealm.setString(2, "($|.*)");
            stmtInsRealm.setLong(3, applicationId);
            stmtInsRealm.setString(4, "RotomOpen Realm");
            stmtInsRealm.setBoolean(5, false);
            stmtInsRealm.executeUpdate();
            Long rotomOpenRealmId = fetchGeneratedKey(stmtInsRealm);

            // --------------------------------------
            // Role
            // --------------------------------------
            stmtInsRole.setString(1, "ROTOM_CURATOR");
            stmtInsRole.setString(2, "Rotom curator");
            stmtInsRole.setBoolean(3, false);
            stmtInsRole.executeUpdate();
            Long curatorRoleId = fetchGeneratedKey(stmtInsRole);

            stmtInsRole.setString(1, "ROTOM_READER");
            stmtInsRole.setString(2, "Rotom reader");
            stmtInsRole.setBoolean(3, false);
            stmtInsRole.executeUpdate();
            Long readerRoleId = fetchGeneratedKey(stmtInsRole);

            Stream.of(Stream.of(READER_PERMISSIONS), Stream.of(CURATOR_PERMISSIONS))
                    .flatMap(s -> s)
                    .distinct().collect(Collectors.toList())
                    .forEach(perm -> {
                        try {
                            stmtInsPermission.setString(1, perm);
                            stmtInsPermission.setString(2, "");
                            stmtInsPermission.setBoolean(3, true);
                            stmtInsPermission.executeUpdate();
                            Long permissionId = fetchGeneratedKey(stmtInsPermission);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    });

            Arrays.asList(READER_PERMISSIONS).forEach(perm -> {
                try {
                    Long permissionId = 0L;
                    stmtSelPermission.setString(1, perm);
                    try (ResultSet rs = stmtSelPermission.executeQuery()) {
                        // the name column of the permission table is unique, only one data can not be get
                        if (rs.next()) {
                            permissionId = rs.getLong("permission_id");
                        }
                    }
                    stmtInsRolePermission.setLong(1, readerRoleId);
                    stmtInsRolePermission.setLong(2, permissionId);
                    stmtInsRolePermission.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            Arrays.asList(CURATOR_PERMISSIONS).forEach(perm -> {
                try {
                    Long permissionId = 0L;
                    stmtSelPermission.setString(1, perm);
                    try (ResultSet rs = stmtSelPermission.executeQuery()) {
                        // the name column of the permission table is unique, only one data can not be get
                        if (rs.next()) {
                            permissionId = rs.getLong("permission_id");
                        }
                    }
                    stmtInsRolePermission.setLong(1, curatorRoleId);
                    stmtInsRolePermission.setLong(2, permissionId);
                    stmtInsRolePermission.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            stmtInsAssignment.setLong(1, readerGroupId);
            stmtInsAssignment.setLong(2, readerRoleId);
            stmtInsAssignment.setLong(3, rotomOpenRealmId);
            stmtInsAssignment.executeUpdate();

            stmtInsAssignment.setLong(1, curatorGroupId);
            stmtInsAssignment.setLong(2, curatorRoleId);
            stmtInsAssignment.setLong(3, rotomOpenRealmId);
            stmtInsAssignment.executeUpdate();
        }
        */
    }
}
