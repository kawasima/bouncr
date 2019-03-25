package net.unit8.bouncr.api.logging;

import enkan.MiddlewareChain;
import enkan.annotation.Middleware;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.data.jpa.EntityManageable;
import enkan.middleware.AbstractWebMiddleware;
import enkan.util.MixinUtils;
import enkan.util.jpa.EntityTransactionManager;
import net.unit8.bouncr.entity.UserAction;

import javax.persistence.EntityManager;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

import static enkan.util.BeanBuilder.builder;

@Middleware(name = "actionLogging", dependencies = {"entityManager"})
public class ActionLoggingMiddleware<NRES> extends AbstractWebMiddleware<HttpRequest, NRES> {
    @Override
    public HttpResponse handle(HttpRequest request, MiddlewareChain<HttpRequest, NRES, ?, ?> chain) {
        request = MixinUtils.mixin(request, ActionRecordable.class);
        ActionRecord actionRecord = new ActionRecord();
        ((ActionRecordable)request).setActionRecord(actionRecord);

        NRES response = chain.next(request);

        if (actionRecord.shouldBeRecorded()) {
            recordAction(request, actionRecord);
        }
        return castToHttpResponse(response);
    }

    protected void recordAction(HttpRequest request, ActionRecord actionRecord) {
        String actor = Optional.ofNullable(actionRecord.getActor())
                .orElseGet(() -> Optional.ofNullable(request.getPrincipal())
                        .map(Principal::getName)
                        .orElse(null));

        if (actor == null) return;

        EntityManager em = ((EntityManageable) request).getEntityManager();
        EntityTransactionManager tx = new EntityTransactionManager(em);
        UserAction userAction = builder(new UserAction())
                .set(UserAction::setActorIp, request.getRemoteAddr())
                .set(UserAction::setActor, actor)
                .set(UserAction::setActionType, actionRecord.getActionType())
                .set(UserAction::setOptions,actionRecord.getDescription())
                    .set(UserAction::setCreatedAt, LocalDateTime.now())
                .build();
        tx.required(() -> {
            em.persist(userAction);
        });

    }
}
