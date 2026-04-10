package net.unit8.bouncr.api.logging;

import enkan.MiddlewareChain;
import enkan.annotation.Middleware;
import enkan.data.Extendable;
import enkan.web.data.HttpRequest;
import enkan.web.data.HttpResponse;
import enkan.web.middleware.WebMiddleware;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.MixinUtils;
import net.unit8.bouncr.api.repository.UserActionRepository;
import net.unit8.bouncr.api.util.PrincipalUtils;
import org.jooq.DSLContext;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

@Middleware(name = "actionLogging", dependencies = {"jooqDslContext"})
public class ActionLoggingMiddleware implements WebMiddleware {
    @Override
    public <NNREQ, NNRES> HttpResponse handle(HttpRequest request, MiddlewareChain<HttpRequest, HttpResponse, NNREQ, NNRES> chain) {
        request = MixinUtils.mixin(request, ActionRecordable.class);
        ActionRecord actionRecord = new ActionRecord();
        ((ActionRecordable)request).setActionRecord(actionRecord);

        HttpResponse response = chain.next(request);

        if (actionRecord.shouldBeRecorded()) {
            recordAction(request, actionRecord);
        }
        return response;
    }

    protected void recordAction(HttpRequest request, ActionRecord actionRecord) {
        String actor = Optional.ofNullable(actionRecord.getActor())
                .orElseGet(() -> Optional.ofNullable(request.getPrincipal())
                        .map(Principal::getName)
                        .orElse(null));

        if (actor == null) return;

        Principal principal = request.getPrincipal();
        if (principal instanceof UserPermissionPrincipal upp && PrincipalUtils.isClientToken(upp)) {
            actor = "client:" + actor;
        }

        DSLContext dsl = null;
        if (request instanceof Extendable e) {
            dsl = e.getExtension("jooqDslContext");
        }
        if (dsl == null) return;

        UserActionRepository repo = new UserActionRepository(dsl);
        repo.insert(
                actionRecord.getActionType().getName(),
                actor,
                request.getRemoteAddr(),
                actionRecord.getDescription(),
                LocalDateTime.now());
    }
}
