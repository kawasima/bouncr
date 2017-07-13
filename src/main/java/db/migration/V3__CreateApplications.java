package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

/**
 * @author kawasima
 */
public class V3__CreateApplications implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE applications("
                    + "application_id IDENTITY,"
                    + "name VARCHAR(100) NOT NULL,"
                    + "PRIMARY KEY(application_id)"
                    + ")");
        }

    }
}
