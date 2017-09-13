package db.migration;

import enkan.Env;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class V999__TestData implements JdbcMigration {
    private static final String INS_OAUTH2_PROVIDER =
            "INSERT INTO oauth2_providers(name, api_key, api_secret, response_type, access_token_endpoint, authorization_base_url,"
                    + "user_info_endpoint, user_id_path)"
                    + "VALUES(?,?,?,?,?,?,?,?)";

    @Override
    public void migrate(Connection connection) throws Exception {
        try(PreparedStatement stmtOauth2Provider = connection.prepareStatement(INS_OAUTH2_PROVIDER)) {
            stmtOauth2Provider.setString(1, "GitHub");
            stmtOauth2Provider.setString(2, Env.getString("CLIENT_ID","xxxx"));
            stmtOauth2Provider.setString(3, Env.getString("CLIENT_SECRET", "xxxx"));
            stmtOauth2Provider.setString(4, "code");
            stmtOauth2Provider.setString(5, "https://github.com/login/oauth/access_token");
            stmtOauth2Provider.setString(6, "https://github.com/login/oauth/authorize");
            stmtOauth2Provider.setString(7, "https://api.github.com/user");
            stmtOauth2Provider.setString(8, "id");
            stmtOauth2Provider.executeUpdate();
            connection.commit();
        } catch(SQLException ex) {
            connection.rollback();
            throw ex;
        }

    }
}
