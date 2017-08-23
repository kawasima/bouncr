package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

/**
 * @author kawasima
 */
public class V1__CreateUsers implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE users("
                    + "user_id IDENTITY not null,"
                    + "account VARCHAR(100) not null,"
                    + "name    VARCHAR(100) not null,"
                    + "email   VARCHAR(100) not null,"
                    + "write_protected BOOLEAN NOT NULL,"
                    + "PRIMARY KEY(user_id)"
                    + ")");
        }
    }
}
