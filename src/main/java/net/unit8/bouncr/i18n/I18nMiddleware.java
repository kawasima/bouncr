package net.unit8.bouncr.i18n;

import enkan.MiddlewareChain;
import enkan.annotation.Middleware;
import enkan.data.ContentNegotiable;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.middleware.AbstractWebMiddleware;
import enkan.util.MixinUtils;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.ext.beans.ResourceBundleModel;
import freemarker.template.Version;
import kotowari.data.TemplatedHttpResponse;
import net.unit8.bouncr.component.BouncrConfiguration;

import javax.inject.Inject;
import java.util.ResourceBundle;

@Middleware(name = "i18n", dependencies = "contentNegotiation")
public class I18nMiddleware<NRES> extends AbstractWebMiddleware<HttpRequest, NRES> {
    @Inject
    private BouncrConfiguration config;

    @Override
    public HttpResponse handle(HttpRequest request, MiddlewareChain<HttpRequest, NRES, ?, ?> chain) {
        ContentNegotiable negotiable = ContentNegotiable.class.cast(MixinUtils.mixin(request, ContentNegotiable.class));
        HttpResponse response = castToHttpResponse(chain.next(request));
        if (TemplatedHttpResponse.class.isInstance(response)) {
            TemplatedHttpResponse tres = TemplatedHttpResponse.class.cast(response);
            ResourceBundle bundle = config.getMessageResource().getBundle(negotiable.getLocale());
            tres.getContext().put("t", new ResourceBundleModel(bundle,
                    new BeansWrapperBuilder(new Version(2,3,23)).build()));
        }
        return response;
    }
}
