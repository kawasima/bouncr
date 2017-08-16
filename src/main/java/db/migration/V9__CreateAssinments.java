package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

/**
 * The entity of role assignments.
 *
 * @author kawasima
 */
public class V9__CreateAssinments implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("CREATE TABLE assignments("
                    + "group_id BIGINT not null,"
                    + "role_id BIGINT not null,"
                    + "realm_id BIGINT not null,"
                    + "PRIMARY KEY(group_id, role_id, realm_id),"
                    + "FOREIGN KEY(group_id) REFERENCES groups(group_id),"
                    + "FOREIGN KEY(role_id) REFERENCES roles(role_id),"
                    + "FOREIGN KEY(realm_id) REFERENCES realms(realm_id)"
                    + ")");
        }

    }
}
