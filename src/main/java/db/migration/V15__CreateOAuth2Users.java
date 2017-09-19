package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.Statement;

import static org.jooq.impl.DSL.*;

public class V15__CreateOAuth2Users implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try(Statement stmt = connection.createStatement()) {
            DSLContext create = DSL.using(connection);
            String ddl = create.createTable(table("oauth2_users"))
                    .column(field("oauth2_provider_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("user_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("oauth2_user_name", SQLDataType.VARCHAR(255).nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("oauth2_provider_id"), field("user_id")),
                            constraint().foreignKey(field("oauth2_provider_id")).references(table("oauth2_providers"), field("oauth2_provider_id")),
                            constraint().foreignKey(field("user_id")).references(table("users"), field("user_id"))
                    ).getSQL();
            stmt.execute(ddl);
        }
    }
}
