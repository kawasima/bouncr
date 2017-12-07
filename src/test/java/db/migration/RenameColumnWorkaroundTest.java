package db.migration;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.SQLDataType;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;
import static org.jooq.impl.DSL.*;

public class RenameColumnWorkaroundTest {
    private String createDDL(DefaultConfiguration config) {
        DSLContext create = DSL.using(config);

        String ddl = create.alterTable(table("oidc_invitations"))
                .renameColumn(field("oidc_sub"))
                .to(field("oidc_payload", SQLDataType.CLOB))
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
        return ddl;
    }

    @Test
    public void testOther() {
        DefaultConfiguration config = new DefaultConfiguration();
        config.setSQLDialect(SQLDialect.POSTGRES);
        assertThat(createDDL(config)).containsIgnoringCase("RENAME COLUMN");
    }

    @Test
    public void testMysql() {
        DefaultConfiguration config = new DefaultConfiguration();
        config.setSQLDialect(SQLDialect.MYSQL);

        assertThat(createDDL(config)).containsIgnoringCase("CHANGE");

    }
}
