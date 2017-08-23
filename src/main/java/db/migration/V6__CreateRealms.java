package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

/**
 * @author kawasima
 */
public class V6__CreateRealms implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE realms("
                    + "realm_id IDENTITY,"
                    + "name VARCHAR(100) NOT NULL,"
                    + "url  VARCHAR(100) NOT NULL,"
                    + "application_id BIGINT NOT NULL,"
                    + "description VARCHAR(100) NOT NULL,"
                    + "write_protected BOOLEAN NOT NULL,"
                    + "PRIMARY KEY (realm_id),"
                    + "FOREIGN KEY (application_id) REFERENCES applications(application_id)"
                    + ")");
        }
    }
}
