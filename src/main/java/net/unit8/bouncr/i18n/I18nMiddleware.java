package net.unit8.bouncr.i18n;

import enkan.MiddlewareChain;
import enkan.annotation.Middleware;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.middleware.AbstractWebMiddleware;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.ext.beans.ResourceBundleModel;
import freemarker.template.Version;
import kotowari.data.TemplatedHttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

@Middleware(name = "i18n")
public class I18nMiddleware extends AbstractWebMiddleware {
    private ResourceBundle bundle;

    public I18nMiddleware() {
        bundle = ResourceBundle.getBundle("messages", UTF8_ENCODING_CONTROL);
    }

    @Override
    public HttpResponse handle(HttpRequest request, MiddlewareChain chain) {
        HttpResponse response = castToHttpResponse(chain.next(request));

        if (TemplatedHttpResponse.class.isInstance(response)) {
            TemplatedHttpResponse tres = TemplatedHttpResponse.class.cast(response);
            tres.getContext().put("t", new ResourceBundleModel(bundle,
                    new BeansWrapperBuilder(new Version(2,3,23)).build()));
        }
        return response;
    }

    private static ResourceBundle.Control UTF8_ENCODING_CONTROL = new ResourceBundle.Control() {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");

            try (InputStream is = loader.getResourceAsStream(resourceName);
                 InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                 BufferedReader reader = new BufferedReader(isr)) {
                return new PropertyResourceBundle(reader);
            }
        }
    };
}
