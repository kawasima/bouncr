package net.unit8.bouncr.proxy;

import enkan.Env;
import enkan.collection.OptionMap;
import enkan.component.builtin.HmacEncoder;
import enkan.component.eclipselink.EclipseLinkEntityManagerProvider;
import enkan.component.flyway.FlywayMigration;
import enkan.component.hikaricp.HikariCPComponent;
import enkan.component.jackson.JacksonBeansConverter;
import enkan.component.metrics.MetricsComponent;
import enkan.config.EnkanSystemFactory;
import enkan.system.EnkanSystem;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.Flake;
import net.unit8.bouncr.component.RealmCache;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.component.config.PasswordPolicy;
import net.unit8.bouncr.entity.*;
import net.unit8.bouncr.proxy.cert.ReloadableTrustManager;
import net.unit8.bouncr.sign.JsonWebToken;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
import java.time.Duration;

import static enkan.component.ComponentRelationship.component;
import static enkan.util.BeanBuilder.builder;

public class BouncrProxyEnkanSystemFactory implements EnkanSystemFactory {
    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Override
    public EnkanSystem create() {
        return EnkanSystem.of(
                "hmac", new HmacEncoder(),
                "config", builder(new BouncrConfiguration())
                        .set(BouncrConfiguration::setPasswordPolicy, builder(new PasswordPolicy())
                                .set(PasswordPolicy::setExpires, Duration.ofDays(10))
                                .build())
                        .build(),
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
                "jackson", new JacksonBeansConverter(),
                "storeprovider", new StoreProvider(),
                "flake", new Flake(),
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
                        "password", Env.get("JDBC_PASSWORD"),
                        "schema", Env.getString("JDBC_SCHEMA", null))),
                "http", builder(new ReverseProxyComponent())
                        .set(ReverseProxyComponent::setHost, Env.getString("HOST", "0.0.0.0"))
                        .set(ReverseProxyComponent::setPort, Env.getInt("PORT", 3000))
                        .set(ReverseProxyComponent::setSslPort, Env.getInt("SSL_PORT", 3002))
                        .set(ReverseProxyComponent::setIoThreads, Env.getInt("IO_THREADS", 4))
                        .set(ReverseProxyComponent::setMaxRequestTime, Env.getInt("MAX_REQUEST_TIME", 30000))
                        .set(ReverseProxyComponent::setSsl, Env.get("SSL_PORT") != null)
                        .set(ReverseProxyComponent::setKeystorePath, Env.getString("KEYSTORE_PATH", ""))
                        .set(ReverseProxyComponent::setKeystorePassword, Env.getString("KEYSTORE_PASSWORD", ""))
                        .build()
        ).relationships(
                component("http").using("storeprovider", "realmCache", "trustManager", "config", "jwt"),
                component("storeprovider").using("config"),
                component("realmCache").using("jpa"),
                component("jpa").using("datasource", "flyway"),
                component("flyway").using("datasource"),
                component("jwt").using("config")
        );
    }
}
