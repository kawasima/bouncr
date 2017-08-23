package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

/**
 * @author kawasima
 */
public class V4__CreateRoles implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE roles("
                    + "role_id IDENTITY,"
                    + "name VARCHAR(100) NOT NULL,"
                    + "description VARCHAR(100) NOT NULL,"
                    + "write_protected BOOLEAN NOT NULL,"
                    + "PRIMARY KEY(role_id)"
                    + ")");
        }

    }
}
