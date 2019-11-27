package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

public class V25__AddRedirectUriToOidcProvider extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try(Statement stmt = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_UPDATABLE)) {
            DSLContext create = DSL.using(connection);
            addRedirectUri(create, stmt);
        }
    }

    private void addRedirectUri(DSLContext create, Statement stmt) throws SQLException {
        String ddl = create.alterTable(table("oidc_providers"))
                .addColumn(field("redirect_uri", SQLDataType.VARCHAR(255)))
                .getSQL();
        stmt.execute(ddl);
    }
}
