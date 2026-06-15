package net.unit8.bouncr.hook.license;

import enkan.web.collection.Headers;
import enkan.web.data.Cookie;
import enkan.data.Extendable;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.hook.Hook;
import org.jooq.DSLContext;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import static enkan.util.ThreadingUtils.some;
import static org.jooq.impl.DSL.*;

public class LicenseDeleteHook implements Hook<RestContext> {
    private LicenseConfig config;

    public LicenseDeleteHook(LicenseConfig config) {
        this.config = config;
    }

    @Override
    public void run(RestContext context) {
        DSLContext dsl = null;
        if (context.getRequest() instanceof Extendable e) {
            dsl = e.getExtension("jooqDslContext");
        }
        if (dsl == null) return;

        LicenseKey licenseKey = some(context.getRequest().getCookies(),
                c -> c.get(config.getCookieName()),
                Cookie::getValue,
                LicenseKey::new).orElse(null);

        if (licenseKey == null) return;

        // Delete user_licenses matching the license key
        // (cascade or manual delete of license_last_activities if needed)
        var deletedIds = dsl.select(field("user_license_id", Long.class))
                .from(table("user_licenses"))
                .where(field("license_key").eq(licenseKey.asBytes()))
                .fetch(rec -> rec.get(field("user_license_id", Long.class)));

        if (!deletedIds.isEmpty()) {
            dsl.deleteFrom(table("license_last_activities"))
                    .where(field("user_license_id").in(deletedIds))
                    .execute();
        }

        dsl.deleteFrom(table("user_licenses"))
                .where(field("license_key").eq(licenseKey.asBytes()))
                .execute();

        Cookie cookie = Cookie.create(config.getCookieName(), licenseKey.asString());
        ZoneId zone = ZoneId.systemDefault();
        Date expires = Date.from(
                ZonedDateTime.of(LocalDate.now()
                        .minusYears(10)
                        .atTime(0, 0), zone)
                        .toInstant());
        cookie.setExpires(expires);
        cookie.setPath("/");
        context.setHeaders(Headers.of("Set-Cookie", cookie.toHttpString()));
    }
}
