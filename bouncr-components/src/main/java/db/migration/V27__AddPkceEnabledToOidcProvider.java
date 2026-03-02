package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.Statement;

public class V27__AddPkceEnabledToOidcProvider extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE oidc_providers ADD pkce_enabled BOOLEAN NOT NULL DEFAULT FALSE");
        }
    }
}
