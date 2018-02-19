package net.unit8.bouncr.middleware;

import enkan.Middleware;
import enkan.MiddlewareChain;
import enkan.data.ContentNegotiable;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import org.seasar.doma.jdbc.NoResultException;

import javax.ws.rs.core.MediaType;

import static enkan.util.BeanBuilder.*;

@enkan.annotation.Middleware(name = "mappingException")
public class MappingExceptionMiddleware <RES, NRES> implements Middleware<HttpRequest, RES, HttpRequest, NRES> {
    @Override
    public RES handle(HttpRequest request, MiddlewareChain<HttpRequest, NRES, ?, ?> chain) {
        try {
            return (RES) chain.next(request);
        } catch (Throwable e) {
            MediaType mediaType = ContentNegotiable.class.cast(request).getMediaType();
            if (e instanceof NoResultException) {
                return (RES) builder(HttpResponse.of(""))
                        .set(HttpResponse::setStatus, 404)
                        .build();
            }

            throw e;
        }
    }
}
