package net.unit8.bouncr.authn;

import javax.sql.DataSource;
import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author kawasima
 */
public class JdbcPasswordLoginBackend implements PasswordLoginBackend {
    private DataSource dataSource;
    private String loginSql;

    public JdbcPasswordLoginBackend(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Principal login(String id, String password) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(loginSql)) {
            stmt.setString(1, id);
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String account = rs.getString(1);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
