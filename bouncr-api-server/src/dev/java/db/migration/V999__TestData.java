package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;

public class V999__TestData extends BaseJavaMigration {
    private static final String INS_OIDC_PROVIDER =
            "INSERT INTO oidc_providers(name, api_key, api_secret, response_type, token_endpoint, authorization_endpoint, scope, token_endpoint_auth_method) "
                    + "VALUES(?,?,?,?,?,?,?,?)";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
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
