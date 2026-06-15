package net.unit8.bouncr.hook.license;

import enkan.web.collection.Headers;
import enkan.web.data.Cookie;
import enkan.data.Extendable;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.hook.Hook;
import net.unit8.bouncr.hook.license.entity.UserLicense;
import org.jooq.DSLContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

public class LicenseUpdateHook implements Hook<RestContext> {
    private final LicenseConfig config;

    public LicenseUpdateHook(LicenseConfig config) {
        this.config = config;
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
            UserLicense userLicense = context.getByType(UserLicense.class).orElseGet(() -> {
                LicenseKey newLicenseKey = LicenseKey.createNew();

                String userAgent = Optional.ofNullable(context.getRequest().getHeaders())
                        .map(headers -> headers.get("User-Agent"))
                        .map(ua -> ua.substring(0, Math.min(ua.length() - 1, 255)))
                        .orElse(null);

                Long userLicenseId = finalDsl.insertInto(table("user_licenses"))
                        .set(field("user_id"), user.id())
                        .set(field("license_key"), newLicenseKey.asBytes())
                        .returningResult(field("user_license_id", Long.class))
                        .fetchOne()
                        .get(field("user_license_id", Long.class));

                finalDsl.insertInto(table("license_last_activities"))
                        .set(field("user_license_id"), userLicenseId)
                        .set(field("user_agent"), userAgent)
                        .set(field("last_used_at"), LocalDateTime.now())
                        .execute();

                Cookie cookie = Cookie.create(config.getCookieName(), newLicenseKey.asString());
                ZoneId zone = ZoneId.systemDefault();
                Date expires = Date.from(
                        ZonedDateTime.of(LocalDate.now()
                                .plusYears(10)
                                .atTime(0, 0), zone)
                                .toInstant());
                cookie.setExpires(expires);
                cookie.setPath("/");
                context.setHeaders(Headers.of("Set-Cookie", cookie.toHttpString()));

                return new UserLicense(userLicenseId, user.id(), newLicenseKey.asBytes());
            });
        });
    }
}
