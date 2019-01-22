package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

public class V22__AlterInvitations extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try(Statement stmt = connection.createStatement()) {
            DSLContext create = DSL.using(connection);
            String ddl = create.alterTable(table("oidc_invitations"))
                    .renameColumn(field("oidc_sub")).to(field("oidc_payload", SQLDataType.CLOB))
                    .getSQL();
            if (create.configuration().dialect() == SQLDialect.MYSQL) {
                Matcher m = Pattern.compile("\\s+RENAME\\s+COLUMN\\s+(\\w+)\\s+TO\\s+", Pattern.CASE_INSENSITIVE).matcher(ddl);
                StringBuffer sb = new StringBuffer();
                if (m.find()) {
                    m.appendReplacement(sb, " change " + m.group(1) + " ");
                    m.appendTail(sb);
                    sb.append(" text not null");
                    ddl = sb.toString();
                }
            }

            stmt.execute(ddl);
        }
    }
}
