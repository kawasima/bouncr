package net.unit8.bouncr.hook.license;

import enkan.data.Cookie;
import enkan.data.Extendable;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.hook.Hook;
import net.unit8.bouncr.hook.license.entity.LicenseLastActivity;
import net.unit8.bouncr.hook.license.entity.UserLicense;
import org.jooq.DSLContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

public class LicenseCheckHook implements Hook<RestContext> {
    private static final ContextKey<UserLicense> USER_LICENSE_KEY = ContextKey.of(UserLicense.class);
    private static final ContextKey<LicenseKey> LICENSE_KEY_KEY = ContextKey.of(LicenseKey.class);

    private LicenseConfig config;

    public LicenseCheckHook(LicenseConfig config) {
        this.config = config;
    }

    private LicenseLastActivity findLastActivity(DSLContext dsl, Long userLicenseId) {
        return dsl.select(
                        field("license_last_activity_id", Long.class),
                        field("user_license_id", Long.class),
                        field("user_agent", String.class),
                        field("last_used_at", LocalDateTime.class))
                .from(table("license_last_activities"))
                .where(field("user_license_id").eq(userLicenseId))
                .fetchOptional(rec -> new LicenseLastActivity(
                        rec.get(field("license_last_activity_id", Long.class)),
                        rec.get(field("user_license_id", Long.class)),
                        rec.get(field("user_agent", String.class)),
                        rec.get(field("last_used_at", LocalDateTime.class))))
                .orElse(null);
    }

    @Override
    public void run(RestContext context) {
        DSLContext dsl = null;
        if (context.getRequest() instanceof Extendable e) {
            dsl = e.getExtension("jooqDslContext");
        }
        if (dsl == null) return;

        DSLContext finalDsl = dsl;
        context.getByType(User.class).ifPresent(user -> {
            final LicenseKey licenseKey = Optional.ofNullable(context.getRequest().getCookies())
                    .map(c -> c.get(config.getCookieName()))
                    .map(Cookie::getValue)
                    .map(LicenseKey::new)
                    .orElse(null);

            final List<UserLicense> licenses = finalDsl.select(
                            field("user_license_id", Long.class),
                            field("user_id", Long.class),
                            field("license_key", byte[].class))
                    .from(table("user_licenses"))
                    .where(field("user_id").eq(user.id()))
                    .fetch(rec -> new UserLicense(
                            rec.get(field("user_license_id", Long.class)),
                            rec.get(field("user_id", Long.class)),
                            rec.get(field("license_key", byte[].class))));

            UserLicense userLicense = licenses.stream()
                    .filter(lic -> new LicenseKey(lic.licenseKey()).equals(licenseKey))
                    .findAny()
                    .orElse(null);

            if (userLicense == null
                    && licenses.size() >= config.getNumOfDevicesPerUser()) {
                context.setMessage(Problem.valueOf(403));
            } else {
                if (userLicense != null) {
                    final LicenseLastActivity lastActivity = findLastActivity(finalDsl, userLicense.id());
                    String userAgent = Optional.ofNullable(context.getRequest().getHeaders())
                            .map(headers -> headers.get("User-Agent"))
                            .map(ua -> ua.substring(0, Math.min(ua.length() - 1, 255)))
                            .orElse(null);

                    if (lastActivity == null) {
                        finalDsl.insertInto(table("license_last_activities"))
                                .set(field("user_license_id"), userLicense.id())
                                .set(field("user_agent"), userAgent)
                                .set(field("last_used_at"), LocalDateTime.now())
                                .execute();
                    } else {
                        finalDsl.update(table("license_last_activities"))
                                .set(field("user_agent"), userAgent)
                                .set(field("last_used_at"), LocalDateTime.now())
                                .where(field("license_last_activity_id").eq(lastActivity.id()))
                                .execute();
                    }
                    context.put(USER_LICENSE_KEY, userLicense);
                }
                context.put(LICENSE_KEY_KEY, licenseKey);
            }
        });
    }
}
