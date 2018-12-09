package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.Statement;

import static org.jooq.impl.DSL.*;

public class V15__CreateOidcUsers implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try(Statement stmt = connection.createStatement()) {
            DSLContext create = DSL.using(connection);
            String ddl = create.createTable(table("oidc_users"))
                    .column(field("oidc_provider_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("oidc_sub", SQLDataType.VARCHAR(255).nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("oidc_provider_id"), field("user_id")),
                            constraint().foreignKey(field("oidc_provider_id")).references(table("oidc_providers"), field("oidc_provider_id")),
                            constraint().foreignKey(field("user_id")).references(table("users"), field("user_id"))
                    ).getSQL();
            stmt.execute(ddl);
        }
    }
}
