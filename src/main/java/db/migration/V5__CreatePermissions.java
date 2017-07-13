package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

/**
 * @author kawasima
 */
public class V5__CreatePermissions implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE permissions("
                    + "permission_id IDENTITY,"
                    + "name VARCHAR(100) NOT NULL,"
                    + "PRIMARY KEY(permission_id)"
                    + ")");
        }
    }
}
