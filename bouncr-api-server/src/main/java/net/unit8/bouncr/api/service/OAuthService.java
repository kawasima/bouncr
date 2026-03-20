package net.unit8.bouncr.api.service;

import enkan.collection.Headers;
import enkan.data.HttpResponse;
import net.unit8.bouncr.data.OAuth2Error;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static enkan.util.BeanBuilder.builder;

public class OAuthService {
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    public HttpResponse errorResponse(OAuth2Error code) {
        return errorResponse(code, null, null);
    }

    public HttpResponse errorResponse(OAuth2Error code, String description, String uri) {
        ObjectNode node = JSON_MAPPER.createObjectNode();
        node.put("error", code.getValue());
        if (description != null) {
            node.put("error_description", description);
        }
        if (uri != null) {
            node.put("error_uri", uri);
        }
        String json = node.toString();
        return builder(HttpResponse.of(json))
                .set(HttpResponse::setHeaders, Headers.of("Content-Type", "application/json"))
                .set(HttpResponse::setStatus, code.getStatusCode())
                .build();
    }
}
