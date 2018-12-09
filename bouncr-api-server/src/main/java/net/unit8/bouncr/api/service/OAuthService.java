package net.unit8.bouncr.api.service;

import enkan.collection.Headers;
import enkan.data.HttpResponse;
import net.unit8.bouncr.entity.OAuth2Error;

import java.util.HashSet;
import java.util.Set;

import static enkan.util.BeanBuilder.builder;

public class OAuthService {
    public HttpResponse errorResponse(OAuth2Error code) {
        return errorResponse(code, null, null);
    }

    public HttpResponse errorResponse(OAuth2Error code, String description, String uri) {
        Set<String> keyValuePairs = new HashSet<>(3);
        keyValuePairs.add("\"error\":\"" + code.getValue() + "\"");
        if (description != null) {
            keyValuePairs.add("\"error_description\":\"" + description + "\"");
        }
        if (uri != null) {
            keyValuePairs.add("\"error_uri\":\"" + uri + "\"");
        }
        String json = "{" + String.join(",", keyValuePairs) + "}";
        return builder(HttpResponse.of(json))
                .set(HttpResponse::setHeaders, Headers.of("Content-Type", "application/json"))
                .set(HttpResponse::setStatus, code.getStatusCode())
                .build();
    }
}
