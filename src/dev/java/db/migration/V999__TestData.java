package db.migration;

import enkan.Env;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class V999__TestData implements JdbcMigration {
    private static final String INS_OAUTH2_PROVIDER =
            "INSERT INTO oidc_providers(name, api_key, api_secret, response_type, token_endpoint, authorization_endpoint, scope, token_endpoint_auth_method) "
                    + "VALUES(?,?,?,?,?,?,?,?)";

    @Override
    public void migrate(Connection connection) throws Exception {
        try(PreparedStatement stmtOauth2Provider = connection.prepareStatement(INS_OAUTH2_PROVIDER)) {
            stmtOauth2Provider.setString(1, "Google");
            stmtOauth2Provider.setString(2, Env.getString("CLIENT_ID","xxxx"));
            stmtOauth2Provider.setString(3, Env.getString("CLIENT_SECRET", "xxxx"));
            stmtOauth2Provider.setString(4, "code");
            stmtOauth2Provider.setString(5, "https://www.googleapis.com/oauth2/v4/token");
            stmtOauth2Provider.setString(6, "https://accounts.google.com/o/oauth2/v2/auth");
            stmtOauth2Provider.setString(7, "openid");
            stmtOauth2Provider.setString(8, "POST");
            stmtOauth2Provider.executeUpdate();
            connection.commit();
        } catch(SQLException ex) {
            connection.rollback();
            throw ex;
        }

    }
}
