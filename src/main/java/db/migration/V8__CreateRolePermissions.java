package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

/**
 * @author kawasima
 */
public class V8__CreateRolePermissions implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("CREATE TABLE role_permissions("
                    + "role_id INTEGER not null,"
                    + "permission_id INTEGER not null,"
                    + "PRIMARY KEY(role_id, permission_id),"
                    + "FOREIGN KEY(role_id) REFERENCES roles(role_id),"
                    + "FOREIGN KEY(permission_id) REFERENCES permissions(permission_id)"
                    + ")");
        }
    }
}
