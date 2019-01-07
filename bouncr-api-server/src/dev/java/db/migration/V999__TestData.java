package db.migration;

import enkan.Env;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class V999__TestData implements JdbcMigration {
    private static final String INS_OIDC_PROVIDER =
            "INSERT INTO oidc_providers(name, api_key, api_secret, response_type, token_endpoint, authorization_endpoint, scope, token_endpoint_auth_method) "
                    + "VALUES(?,?,?,?,?,?,?,?)";

    @Override
    public void migrate(Connection connection) throws Exception {
        /*
        try(PreparedStatement stmtOidcProvider = connection.prepareStatement(INS_OIDC_PROVIDER)) {
            stmtOidcProvider.setString(1, "Google");
            stmtOidcProvider.setString(2, Env.getString("CLIENT_ID","xxxx"));
            stmtOidcProvider.setString(3, Env.getString("CLIENT_SECRET", "xxxx"));
            stmtOidcProvider.setString(4, "code");
            stmtOidcProvider.setString(5, "https://www.googleapis.com/oauth2/v4/token");
            stmtOidcProvider.setString(6, "https://accounts.google.com/o/oauth2/v2/auth");
            stmtOidcProvider.setString(7, "openid");
            stmtOidcProvider.setString(8, "POST");
            stmtOidcProvider.executeUpdate();
            connection.commit();
        } catch(SQLException ex) {
            connection.rollback();
            throw ex;
        }
*/
    }
}
