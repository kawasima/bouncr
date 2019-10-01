package net.unit8.bouncr.component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MessageResource {
    private Map<String, ResourceBundle> bundles = new HashMap<>();
    private ConcurrentHashMap<String, MessageFormat> formats = new ConcurrentHashMap<>();

    public MessageResource(Set<Locale> supportLocales) {
        supportLocales.stream().forEach(locale ->
                bundles.computeIfAbsent(locale.getLanguage(), $ ->
                        ResourceBundle.getBundle("messages", locale, UTF8_ENCODING_CONTROL)));
    }

    public ResourceBundle getBundle(Locale locale) {
        if (locale == null) locale = Locale.getDefault();
        String language = locale.getLanguage();
        if (!bundles.containsKey(language)) {
            language = "en";
        }
        return bundles.get(language);
    }

    public String renderMessage(Locale locale, String key, Object... params) {
        ResourceBundle bundle = getBundle(locale);
        String messageTemplate = bundle.getString(key);
        if (params.length == 0) {
            return messageTemplate;
        }
        MessageFormat format = formats.computeIfAbsent(locale + "/" + key, k ->
                new MessageFormat(messageTemplate, locale));
        return format.format(params);
    }

    private static ResourceBundle.Control UTF8_ENCODING_CONTROL = new ResourceBundle.Control() {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");

            try (InputStream is = loader.getResourceAsStream(resourceName);
                 InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(isr)) {
                return new PropertyResourceBundle(reader);
            }
        }
    };

}
