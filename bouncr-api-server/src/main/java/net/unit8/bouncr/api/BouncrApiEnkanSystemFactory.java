package net.unit8.bouncr.api;

import enkan.Env;
import enkan.collection.OptionMap;
import enkan.component.ApplicationComponent;
import enkan.component.builtin.HmacEncoder;
import enkan.component.eclipselink.EclipseLinkEntityManagerProvider;
import enkan.component.flyway.FlywayMigration;
import enkan.component.hikaricp.HikariCPComponent;
import enkan.component.jackson.JacksonBeansConverter;
import enkan.component.jetty.JettyComponent;
import enkan.component.metrics.MetricsComponent;
import enkan.config.EnkanSystemFactory;
import enkan.system.EnkanSystem;
import kotowari.restful.component.BeansValidator;
import net.unit8.bouncr.api.hook.GrantBouncrUserRole;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.Flake;
import net.unit8.bouncr.component.RealmCache;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.component.config.HookPoint;
import net.unit8.bouncr.component.config.PasswordPolicy;
import net.unit8.bouncr.entity.*;
import net.unit8.bouncr.sign.JsonWebToken;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
import java.time.Duration;
import java.util.Objects;

import static enkan.component.ComponentRelationship.component;
import static enkan.util.BeanBuilder.builder;

public class BouncrApiEnkanSystemFactory implements EnkanSystemFactory {
    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Override
    public EnkanSystem create() {
        BouncrConfiguration config = builder(new BouncrConfiguration())
                .set(BouncrConfiguration::setPasswordPolicy,
                        builder(new PasswordPolicy())
                                .set(PasswordPolicy::setExpires, Duration.ofDays(180))
                                .build())
                .build();
        config.getHookRepo().register(HookPoint.BEFORE_CREATE_USER, new GrantBouncrUserRole());

        return EnkanSystem.of(
                "hmac", new HmacEncoder(),
                "config", config,
                "validator", new BeansValidator(),
                "converter", new JacksonBeansConverter(),
                "jpa", builder(new EclipseLinkEntityManagerProvider())
                        .set(EclipseLinkEntityManagerProvider::registerClass, Application.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, Realm.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, User.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, Group.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, Role.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, Permission.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, RolePermission.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, Assignment.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, UserProfileField.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, UserProfileValue.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, UserProfileVerification.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, PasswordCredential.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, PasswordResetChallenge.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, UserSession.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, UserAction.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, OtpKey.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, UserLock.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, Invitation.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, GroupInvitation.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, OidcInvitation.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, OidcProvider.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, OidcUser.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, OidcApplication.class)
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
                        "uri", Env.getString("JDBC_URL", "jdbc:h2:mem:test"),
                        "username", Env.get("JDBC_USER"),
                        "password", Env.get("JDBC_PASSWORD"))),
                "app", new ApplicationComponent("net.unit8.bouncr.api.BouncrApplicationFactory"),
                "http", builder(new JettyComponent())
                        .set(JettyComponent::setPort, Env.getInt("PORT", 3005))
                        .build()
        ).relationships(
                component("http").using("app"),
                component("app").using("config", "storeprovider", "realmCache", "jpa", "jwt",
                        "validator", "converter", "metrics"),
                component("storeprovider").using("config"),
                component("realmCache").using("jpa"),
                component("jpa").using("datasource", "flyway"),
                component("flyway").using("datasource"),
                component("jwt").using("config")
        );
    }
}
