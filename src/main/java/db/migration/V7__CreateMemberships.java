package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

/**
 * @author kawasima
 */
public class V7__CreateMemberships implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("CREATE TABLE memberships("
                    + "user_id INTEGER not null,"
                    + "group_id INTEGER not null,"
                    + "PRIMARY KEY(user_id, group_id)"
                    + "FOREIGN KEY(user_id) REFERENCES users(user_id),"
                    + "FOREIGN KEY(group_id) REFERENCES groups(group_id),"
                    + ")");
        }
    }
}
