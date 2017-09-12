package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

public class V19__CreateUserSessions implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try(Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE user_sessions("
                    + "user_session_id IDENTITY,"
                    + "user_id BIGINT NOT NULL,"
                    + "token VARCHAR(36) NOT NULL,"
                    + "user_agent VARCHAR(255),"
                    + "created_at TIMESTAMP NOT NULL,"
                    + "PRIMARY KEY (user_session_id),"
                    + "FOREIGN KEY (user_id) REFERENCES users(user_id)"
                    + ")");
        }
    }
}
