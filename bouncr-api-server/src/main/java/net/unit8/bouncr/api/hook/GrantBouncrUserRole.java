package net.unit8.bouncr.api.hook;

import enkan.data.Extendable;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.api.repository.UserRepository;
import net.unit8.bouncr.data.User;
import net.unit8.bouncr.hook.Hook;
import org.jooq.DSLContext;

import static org.jooq.impl.DSL.*;

public class GrantBouncrUserRole implements Hook<RestContext> {
    @Override
    public void run(RestContext context) {
        DSLContext dsl = null;
        if (context.getRequest() instanceof Extendable e) {
            dsl = e.getExtension("jooqDslContext");
        }
        if (dsl == null) return;

        DSLContext finalDsl = dsl;
        context.getByType(User.class).ifPresent(user -> {
            var groupId = finalDsl.select(field("group_id", Long.class))
                    .from(table("groups"))
                    .where(field("name").eq("BOUNCR_USER"))
                    .fetchOptional(rec -> rec.get(field("group_id", Long.class)));
            groupId.ifPresent(gid -> {
                UserRepository repo = new UserRepository(finalDsl);
                repo.addToGroup(user.id(), gid);
            });
        });
    }
}
