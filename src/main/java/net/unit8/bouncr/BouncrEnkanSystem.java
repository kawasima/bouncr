package net.unit8.bouncr;

import enkan.Env;
import enkan.collection.OptionMap;
import enkan.component.ApplicationComponent;
import enkan.component.builtin.HmacEncoder;
import enkan.component.eclipselink.EclipseLinkEntityManagerProvider;
import enkan.component.flyway.FlywayMigration;
import enkan.component.hikaricp.HikariCPComponent;
import enkan.component.jackson.JacksonBeansConverter;
import enkan.component.metrics.MetricsComponent;
import enkan.config.EnkanSystemFactory;
import enkan.system.EnkanSystem;
import kotowari.restful.component.BeansValidator;
import net.unit8.bouncr.cert.CertificateProvider;
import net.unit8.bouncr.cert.ReloadableTrustManager;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.Flake;
import net.unit8.bouncr.component.RealmCache;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.component.config.PasswordPolicy;
import net.unit8.bouncr.proxy.ReverseProxyComponent;
import net.unit8.bouncr.sign.JsonWebToken;
import net.unit8.bouncr.web.entity.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.persistence.config.PersistenceUnitProperties;

import java.security.Security;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static enkan.component.ComponentRelationship.component;
import static enkan.util.BeanBuilder.builder;

/**
 * An EnkanSystem for Bouncr application.
 *
 * @author kawasima
 */
public class BouncrEnkanSystem implements EnkanSystemFactory {
    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Override
    public EnkanSystem create() {
        Map<String, Object> jpaProperties = new HashMap<>(Map.of(PersistenceUnitProperties.SESSION_CUSTOMIZER, CaseConvertCustomizer.class.getName()));
        return EnkanSystem.of(
                "hmac", new HmacEncoder(),
                "config", builder(new BouncrConfiguration())
                        .set(BouncrConfiguration::setPasswordPolicy, builder(new PasswordPolicy())
                                .set(PasswordPolicy::setExpires, Duration.ofDays(10))
                                .build())
                        .build(),
                "jpa", builder(new EclipseLinkEntityManagerProvider())
//                        .set(EclipseLinkEntityManagerProvider::setJpaProperties, jpaProperties)
                        .set(EclipseLinkEntityManagerProvider::registerClass, Application.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, Realm.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, User.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, Membership.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, Group.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, Role.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, Permission.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, RolePermission.class)
                        .set(EclipseLinkEntityManagerProvider::registerClass, Assignment.class)
                        .build(),
                "jackson", new JacksonBeansConverter(),
                "validator", new BeansValidator(),
                "storeprovider", new StoreProvider(),
                "flake", new Flake(),
                "certificate", new CertificateProvider(),
                "trustManager", builder(new ReloadableTrustManager())
                        .set(ReloadableTrustManager::setTruststorePath, Env.getString("TRUSTSTORE_PATH", ""))
                        .set(ReloadableTrustManager::setTruststorePassword, Env.getString("TRUSTSTORE_PASSWORD", ""))
                        .build(),
                "jwt", new JsonWebToken(),
                "realmCache", new RealmCache(),
                "flyway", new FlywayMigration(),
                "metrics", new MetricsComponent(),
                "datasource", new HikariCPComponent(OptionMap.of(
                        "uri", Env.getString("JDBC_URL", "jdbc:h2:mem:test"),
                        "username", Env.get("JDBC_USER"),
                        "password", Env.get("JDBC_PASSWORD"))),
                "app", new ApplicationComponent("net.unit8.bouncr.BouncrApplicationFactory"),
                "http", builder(new ReverseProxyComponent())
                        .set(ReverseProxyComponent::setPort, Env.getInt("PORT", 3000))
                        .set(ReverseProxyComponent::setSslPort, Env.getInt("SSL_PORT", 3002))
                        .set(ReverseProxyComponent::setSsl, Env.get("SSL_PORT") != null)
                        .set(ReverseProxyComponent::setKeystorePath, Env.getString("KEYSTORE_PATH", ""))
                        .set(ReverseProxyComponent::setKeystorePassword, Env.getString("KEYSTORE_PASSWORD", ""))
                        .build()
        ).relationships(
                component("http").using("app", "storeprovider", "realmCache", "trustManager", "config", "jwt"),
                component("app").using(
                        "storeprovider", "datasource", "jpa", "jackson", "metrics", "validator",
                        "realmCache", "config", "jwt", "certificate", "trustManager"),
                component("storeprovider").using("config"),
                component("realmCache").using("jpa"),
                component("jpa").using("datasource", "flyway"),
                component("flyway").using("datasource"),
                component("certificate").using("flake", "config"),
                component("jwt").using("config")
        );
    }
}
