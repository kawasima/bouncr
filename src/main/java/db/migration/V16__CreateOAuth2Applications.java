package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

public class V16__CreateOAuth2Applications implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try(Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE oauth2_applications(" +
                    "oauth2_application_id IDENTITY," +
                    "name VARCHAR(100) NOT NULL," +
                    "client_id VARCHAR(100) NOT NULL," +
                    "client_secret VARCHAR(100) NOT NULL," +
                    "home_url VARCHAR(100) NOT NULL," +
                    "callback_url VARCHAR(100) NOT NULL," +
                    "description VARCHAR(255)," +
                    "PRIMARY KEY(oauth2_application_id)" +
                    ")");
        }
    }
}
