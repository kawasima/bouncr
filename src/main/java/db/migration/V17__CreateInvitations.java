package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.Statement;

import static org.jooq.impl.DSL.*;

public class V17__CreateInvitations implements JdbcMigration {

    @Override
    public void migrate(Connection connection) throws Exception {
        try(Statement stmt = connection.createStatement()) {
            DSLContext create = DSL.using(connection);
            String ddl = create.createTable(table("invitations"))
                    .column(field("invitation_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("email", SQLDataType.VARCHAR(100)))
                    .column(field("code", SQLDataType.VARCHAR(8)))
                    .column(field("invited_at", SQLDataType.TIMESTAMP.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("invitation_id")),
                            constraint().unique(field("code")),constraint()
                    ).getSQL();
            stmt.execute(ddl);

            ddl = create.createTable(table("group_invitations"))
                    .column(field("group_invitation_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("invitation_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("group_id", SQLDataType.BIGINT.nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("group_invitation_id")),
                            constraint().foreignKey(field("invitation_id")).references(table("invitations"), field("invitation_id")),
                            constraint().foreignKey(field("group_id")).references(table("groups"), field("group_id"))
                    ).getSQL();
            stmt.execute(ddl);

            ddl = create.createTable(table("oauth2_invitations"))
                    .column(field("oauth2_invitation_id", SQLDataType.BIGINT.identity(true)))
                    .column(field("invitation_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("oauth2_provider_id", SQLDataType.BIGINT.nullable(false)))
                    .column(field("oauth2_user_name", SQLDataType.VARCHAR(255).nullable(false)))
                    .constraints(
                            constraint().primaryKey(field("oauth2_invitation_id")),
                            constraint().foreignKey(field("invitation_id")).references(table("invitations"), field("invitation_id")),
                            constraint().foreignKey(field("oauth2_provider_id")).references(table("oauth2_providers"), field("oauth2_provider_id"))
                    ).getSQL();
            stmt.execute(ddl);

        }
    }
}
