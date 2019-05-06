package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

public class V24__AddAccountLowerToUser extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try(Statement stmt = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_UPDATABLE)) {
            DSLContext create = DSL.using(connection);
            // Users.Account
            addAccountLower(create, stmt);
            fillAccountLower(create, stmt);
            notNullAccountLower(create, stmt);

            // Groups.Name
            addNameLower(create, stmt, "groups");
            fillNameLower(create, stmt, "groups", "group_id");
            notNullNameLower(create, stmt, "groups");

            // Roles.Name
            addNameLower(create, stmt, "roles");
            fillNameLower(create, stmt, "roles", "role_id");
            notNullNameLower(create, stmt, "roles");

            // Permissions.Name
            addNameLower(create, stmt, "permissions");
            fillNameLower(create, stmt, "permissions", "permission_id");
            notNullNameLower(create, stmt, "permissions");

            // Applications.Name
            addNameLower(create, stmt, "applications");
            fillNameLower(create, stmt, "applications", "application_id");
            notNullNameLower(create, stmt, "applications");

            // Realms.Name
            addNameLower(create, stmt, "realms");
            fillNameLower(create, stmt, "realms", "realm_id");
            notNullNameLower(create, stmt, "realms");

            // OidcApplications.Name
            addNameLower(create, stmt, "oidc_applications");
            fillNameLower(create, stmt, "oidc_applications", "oidc_application_id");
            notNullNameLower(create, stmt, "oidc_applications");

            // OidcProviders.Name
            addNameLower(create, stmt, "oidc_providers");
            fillNameLower(create, stmt, "oidc_providers", "oidc_provider_id");
            notNullNameLower(create, stmt, "oidc_providers");
        }
    }

    private void addAccountLower(DSLContext create, Statement stmt) throws SQLException {
        String ddl = create.alterTable(table("users"))
                .addColumn("account_lower", SQLDataType.VARCHAR(100))
                .getSQL();
        stmt.execute(ddl);
    }

    private void fillAccountLower(DSLContext create, Statement stmt) throws SQLException {
        String sql = create.select(field("user_id"), field("account"), field("account_lower"))
                .from(table("users"))
                .getSQL();
        int r = 1;
        try (ResultSet rs = stmt.executeQuery(sql)) {
            while(rs.next()) {
                String account = rs.getString("account");
                rs.updateString("account_lower", account.toLowerCase(Locale.US));
                rs.updateRow();
                if (++r % 1000 == 0) {
                    stmt.getConnection().commit();
                }
            }
        }
    }

    private void notNullAccountLower(DSLContext create, Statement stmt) throws SQLException {
        String sql = create.alterTable(table("users"))
                .alterColumn("account_lower")
                .setNotNull()
                .getSQL();
        stmt.execute(sql);
    }

    private void addNameLower(DSLContext create, Statement stmt, String tableName) throws SQLException {
        String ddl = create.alterTable(table(tableName))
                .addColumn("name_lower", SQLDataType.VARCHAR(100))
                .getSQL();
        stmt.execute(ddl);
    }

    private void fillNameLower(DSLContext create, Statement stmt, String tableName, String pkColName) throws SQLException {
        String sql = create.select(field(pkColName), field("name"), field("name_lower"))
                .from(table(tableName))
                .getSQL();
        int r = 1;
        try (ResultSet rs = stmt.executeQuery(sql)) {
            while(rs.next()) {
                String account = rs.getString("name");
                rs.updateString("name_lower", account.toLowerCase(Locale.US));
                rs.updateRow();
                if (++r % 1000 == 0) {
                    stmt.getConnection().commit();
                }
            }
        }
    }

    private void notNullNameLower(DSLContext create, Statement stmt, String tableName) throws SQLException {
        String sql = create.alterTable(table(tableName))
                .alterColumn("name_lower")
                .setNotNull()
                .getSQL();
        stmt.execute(sql);
    }

}
