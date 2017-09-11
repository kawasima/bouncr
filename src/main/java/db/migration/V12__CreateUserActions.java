package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

public class V12__CreateUserActions implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try(Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE actions("
                    + "action_id IDENTITY,"
                    + "name VARCHAR(100) NOT NULL,"
                    + "PRIMARY KEY (action_id)"
                    + ")");

            stmt.execute("CREATE TABLE user_actions("
                    + "user_action_id IDENTITY,"
                    + "action_id BIGINT NOT NULL,"
                    + "actor VARCHAR(100),"
                    + "actor_ip VARCHAR(50),"
                    + "options CLOB,"
                    + "created_at TIMESTAMP NOT NULL,"
                    + "PRIMARY KEY (user_action_id)"
                    + ")");
        }
    }
}
