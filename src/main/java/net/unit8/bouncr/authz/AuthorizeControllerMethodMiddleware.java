package net.unit8.bouncr.authz;

import enkan.MiddlewareChain;
import enkan.annotation.Middleware;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.data.PrincipalAvailable;
import enkan.data.Routable;
import enkan.middleware.AbstractWebMiddleware;
import enkan.security.UserPrincipal;

import javax.annotation.security.RolesAllowed;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Stream;

import static enkan.util.BeanBuilder.builder;

@Middleware(name = "authorizeControllerMethod", dependencies = "routing")
public class AuthorizeControllerMethodMiddleware extends AbstractWebMiddleware {
    @Override
    public HttpResponse handle(HttpRequest request, MiddlewareChain chain) {
        Method m = ((Routable) request).getControllerMethod();
        Optional<UserPrincipal> principal = Stream.of(((PrincipalAvailable) request).getPrincipal())
                .filter(UserPrincipal.class::isInstance)
                .map(UserPrincipal.class::cast)
                .findAny();

        RolesAllowed rolesAllowed = m.getAnnotation(RolesAllowed.class);
        if (rolesAllowed != null) {
            if (!principal.isPresent() || !Stream.of(rolesAllowed.value())
                    .anyMatch(permission -> principal.filter(p -> p.hasPermission(permission)).isPresent())) {
                return builder(HttpResponse.of("Not allowed"))
                        .set(HttpResponse::setStatus, 403)
                        .build();
            }
        }
        return HttpResponse.class.cast(chain.next(request));
    }
}
