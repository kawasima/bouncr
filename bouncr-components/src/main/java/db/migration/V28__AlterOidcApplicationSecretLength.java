package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.Statement;

import static org.jooq.impl.DSL.*;

/**
 * Extend client_secret column to accommodate PBKDF2 hash (Base64 encoded, ~344 chars).
 * The original VARCHAR(100) was sufficient for plaintext secrets but not for hashed values.
 */
public class V28__AlterOidcApplicationSecretLength extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        DSLContext create = DSL.using(connection);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(create.alterTable(table("oidc_applications"))
                    .alterColumn(field("client_secret"))
                    .set(SQLDataType.VARCHAR(512).nullable(false))
                    .getSQL());
        }
    }
}
