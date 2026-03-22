package net.unit8.bouncr.api;

import enkan.Env;
import enkan.collection.OptionMap;
import enkan.component.ApplicationComponent;
import enkan.component.builtin.HmacEncoder;
import enkan.component.jooq.JooqProvider;
import enkan.component.flyway.FlywayMigration;
import enkan.component.hikaricp.HikariCPComponent;
import enkan.component.jackson.JacksonBeansConverter;
import enkan.component.jetty.JettyComponent;
import enkan.component.metrics.MetricsComponent;
import enkan.config.EnkanSystemFactory;
import enkan.system.EnkanSystem;
import net.unit8.bouncr.api.hook.GrantBouncrUserRole;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.Flake;
import net.unit8.bouncr.component.RealmCache;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.component.config.HookPoint;
import net.unit8.bouncr.component.config.PasswordPolicy;
import net.unit8.bouncr.sign.JsonWebToken;
import org.jooq.SQLDialect;

import java.time.Duration;
import java.util.Objects;

import static enkan.component.ComponentRelationship.component;
import static enkan.util.BeanBuilder.builder;

/**
 * Development system factory. Uses MemoryStore (no Redis) and H2 in-memory DB by default.
 * Used by ReplMain (mvn exec:java -Pdev).
 */
public class BouncrDevEnkanSystemFactory implements EnkanSystemFactory {
    @Override
    public EnkanSystem create() {
        String jdbcUrl = Env.getString("JDBC_URL", "jdbc:h2:mem:test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        SQLDialect sqlDialect = jdbcUrl.startsWith("jdbc:postgresql") ? SQLDialect.POSTGRES : SQLDialect.H2;

        BouncrConfiguration config = builder(new BouncrConfiguration())
                .set(BouncrConfiguration::setInternalSigningKey,
                        Env.getString("internal.signing.key", null))
                .set(BouncrConfiguration::setPasswordPolicy,
                        builder(new PasswordPolicy())
                                .set(PasswordPolicy::setExpires, Duration.ofDays(180))
                                .build())
                .build();
        GrantBouncrUserRole grantBouncrUserRole = new GrantBouncrUserRole();
        config.getHookRepo().register(HookPoint.BEFORE_CREATE_USER, grantBouncrUserRole);
        config.getHookRepo().register(HookPoint.BEFORE_SIGN_UP, grantBouncrUserRole);

        // StoreProvider defaults to MemoryStore for all stores — no Redis needed
        return EnkanSystem.of(
                "hmac", new HmacEncoder(),
                "config", config,
                "converter", new JacksonBeansConverter(),
                "jooq", builder(new JooqProvider())
                        .set(JooqProvider::setDialect, sqlDialect)
                        .build(),
                "storeprovider", new StoreProvider(),
                "flake", new Flake(),
                "jwt", new JsonWebToken(),
                "realmCache", new RealmCache(),
                "flyway", builder(new FlywayMigration())
                        .set(FlywayMigration::setCleanBeforeMigration, Objects.equals(Env.getString("CLEAR_SCHEMA", "false"), "true"))
                        .build(),
                "metrics", new MetricsComponent(),
                "datasource", new HikariCPComponent(OptionMap.of(
                        "uri", jdbcUrl,
                        "username", Env.get("JDBC_USER"),
                        "password", Env.get("JDBC_PASSWORD"),
                        "schema", Env.getString("JDBC_SCHEMA", null))),
                "app", new ApplicationComponent<>("net.unit8.bouncr.api.BouncrApplicationFactory"),
                "http", builder(new JettyComponent())
                        .set(JettyComponent::setPort, Env.getInt("PORT", 3005))
                        .build()
        ).relationships(
                component("http").using("app"),
                component("app").using("config", "storeprovider", "realmCache", "jooq", "jwt",
                        "converter", "metrics"),
                component("storeprovider").using("config"),
                component("realmCache").using("jooq", "flyway"),
                component("jooq").using("datasource"),
                component("flyway").using("datasource"),
                component("jwt").using("config")
        );
    }
}
