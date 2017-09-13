package net.unit8.bouncr.web;

import org.seasar.doma.jdbc.Config;
import org.seasar.doma.jdbc.EntityListenerProvider;
import org.seasar.doma.jdbc.Naming;
import org.seasar.doma.jdbc.dialect.Dialect;
import org.seasar.doma.jdbc.dialect.H2Dialect;
import org.seasar.doma.jdbc.entity.EntityListener;

import javax.sql.DataSource;
import java.util.function.Supplier;

/**
 * A configuration file for doma.
 *
 * @author kawasima
 */
public class DomaConfig implements Config{
    @Override
    public DataSource getDataSource() {
        return null;
    }

    @Override
    public Dialect getDialect() {
        return new H2Dialect();
    }

    @Override
    public Naming getNaming() {
        return Naming.SNAKE_LOWER_CASE;
    }
}
