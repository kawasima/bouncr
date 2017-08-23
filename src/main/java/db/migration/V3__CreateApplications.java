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
                    + "virtual_path VARCHAR(50) NOT NULL,"
                    + "pass_to VARCHAR(255) NOT NULL,"
                    + "top_page VARCHAR(255) NOT NULL,"
                    + "description VARCHAR(100) NOT NULL,"
                    + "write_protected BOOLEAN NOT NULL,"
                    + "PRIMARY KEY(application_id)"
                    + ")");
        }

    }
}
