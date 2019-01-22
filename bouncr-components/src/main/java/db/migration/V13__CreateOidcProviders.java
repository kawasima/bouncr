package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.Statement;

import static org.jooq.impl.DSL.*;

public class V13__CreateOidcProviders extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try(Statement stmt = connection.createStatement()) {
            DSLContext create = DSL.using(connection);
            String ddl = create.createTable(table("oidc_providers"))
                    .column(field("oidc_provider_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("name", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("api_key", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("api_secret", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("scope", SQLDataType.VARCHAR(100)))
                    .column(field("response_type", SQLDataType.VARCHAR(100)))
                    .column(field("authorization_endpoint", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("token_endpoint", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("token_endpoint_auth_method", SQLDataType.VARCHAR(10).nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("oidc_provider_id")),
                            constraint().unique(field("name"))
                            ).getSQL();
            stmt.execute(ddl);
        }
    }
}
