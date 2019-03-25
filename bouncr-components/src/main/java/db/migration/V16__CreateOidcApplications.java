package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.Statement;

import static org.jooq.impl.DSL.*;

public class V16__CreateOidcApplications extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try(Statement stmt = connection.createStatement()) {
            DSLContext create = DSL.using(connection);
            String ddl = create.createTable(table("oidc_applications"))
                    .column(field("oidc_application_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("name", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("client_id", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("client_secret", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("private_key", SQLDataType.BLOB.nullable(false)))
                    .column(field("public_key", SQLDataType.BLOB.nullable(false)))
                    .column(field("home_url", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("callback_url", SQLDataType.VARCHAR(100).nullable(false)))
                    .column(field("description", SQLDataType.VARCHAR(255).nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("oidc_application_id")),
                            constraint().unique(field("name"))
                    )
                    .getSQL();

            stmt.execute(ddl);

            ddl = create.createTable(table("oidc_application_scopes"))
                    .column(field("oidc_application_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("permission_id", SQLDataType.BIGINT.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("oidc_application_id"), field("permission_id")),
                            constraint().foreignKey(field("oidc_application_id"))
                                    .references(table("oidc_applications"), field("oidc_application_id")).onDeleteCascade(),
                            constraint().foreignKey(field("permission_id"))
                                    .references(table("permissions"), field("permission_id")).onDeleteCascade()
                    )
                    .getSQL();
            stmt.execute(ddl);
        }
    }
}
