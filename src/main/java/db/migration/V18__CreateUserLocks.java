package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

public class V18__CreateUserLocks implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try(Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE user_locks("
                    + "user_id BIGINT,"
                    + "locked_at TIMESTAMP NOT NULL,"
                    + "PRIMARY KEY (user_id),"
                    + "FOREIGN KEY (user_id) REFERENCES users(user_id)"
                    + ")");
        }
    }
}
