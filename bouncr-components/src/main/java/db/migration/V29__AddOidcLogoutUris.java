package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Statement;

public class V29__AddOidcLogoutUris extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        try (Statement stmt = context.getConnection().createStatement()) {
            stmt.execute("""
                    ALTER TABLE oidc_applications
                    ADD COLUMN backchannel_logout_uri VARCHAR(2048)
                    """);
            stmt.execute("""
                    ALTER TABLE oidc_applications
                    ADD COLUMN frontchannel_logout_uri VARCHAR(2048)
                    """);
        }
    }
}
