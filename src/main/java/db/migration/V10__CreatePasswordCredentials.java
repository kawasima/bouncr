package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

public class V10__CreatePasswordCredentials implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("CREATE TABLE password_credentials("
                    + "user_id BIGINT not null,"
                    + "password BINARY(32) not null,"
                    + "salt VARCHAR(16) not null,"
                    + "PRIMARY KEY(user_id),"
                    + "FOREIGN KEY(user_id) REFERENCES users(user_id)"
                    + ")");
        }

    }
}
