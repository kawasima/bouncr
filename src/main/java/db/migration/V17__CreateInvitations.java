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

            stmt.execute("CREATE TABLE group_invitations("
                    + "group_invitation_id IDENTITY,"
                    + "invitation_id BIGINT NOT NULL,"
                    + "group_id BIGINT NOT NULL,"
                    + "PRIMARY KEY(group_invitation_id),"
                    + "FOREIGN KEY(invitation_id) REFERENCES invitations(invitation_id),"
                    + "FOREIGN KEY(group_id) REFERENCES groups(group_id)"
                    + ")"
            );

            stmt.execute("CREATE TABLE oauth2_invitations("
                    + "oauth2_invitation_id IDENTITY,"
                    + "invitation_id BIGINT NOT NULL,"
                    + "oauth2_provider_id BIGINT NOT NULL,"
                    + "oauth2_user_name VARCHAR(255) NOT NULL,"
                    + "PRIMARY KEY(oauth2_invitation_id),"
                    + "FOREIGN KEY(invitation_id) REFERENCES invitations(invitation_id),"
                    + "FOREIGN KEY(oauth2_provider_id) REFERENCES oauth2_providers(oauth2_provider_id)"
                    + ")"
            );

        }
    }
}
