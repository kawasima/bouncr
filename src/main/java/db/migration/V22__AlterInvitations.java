package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.Statement;

import static org.jooq.impl.DSL.*;

public class V22__AlterInvitations implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        try(Statement stmt = connection.createStatement()) {
            DSLContext create = DSL.using(connection);
            String ddl = create.alterTable(table("oidc_invitations"))
                    .renameColumn(field("oidc_sub")).to(field("oidc_payload", SQLDataType.CLOB))
                    .getSQL();
            stmt.execute(ddl);
        }
    }
}
