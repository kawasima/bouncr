package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

public class V17__CreateInvitations implements JdbcMigration {

    @Override
    public void migrate(Connection connection) throws Exception {
        try(Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE invitations("
                    + "invitation_id IDENTITY,"
                    + "email VARCHAR(100),"
                    + "code  VARCHAR(8) NOT NULL,"
                    + "invited_at TIMESTAMP NOT NULL,"
                    + "PRIMARY KEY(invitation_id)"
                    + ")"
            );
        }
    }
}
