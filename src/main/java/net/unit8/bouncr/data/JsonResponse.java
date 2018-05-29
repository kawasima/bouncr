package net.unit8.bouncr.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import enkan.collection.Headers;
import enkan.collection.Multimap;
import enkan.data.DefaultHttpResponse;

import java.util.Arrays;
import java.util.Map;

public class JsonResponse extends DefaultHttpResponse {
    static ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.registerModule(new JavaTimeModule());
    }
    protected JsonResponse(int status, Headers headers) {
        super(status, headers);
    }

    public static <T> JsonResponse fromEntity(T object) {
        try {
            String json = mapper.writeValueAsString(object);
            JsonResponse response = new JsonResponse(200, Headers.of("Content-Type", "application/json"));
            response.setBody(json);
            return response;
        } catch (JsonProcessingException e) {
            return serverError(e);
        }
    }

    public static JsonResponse badRequest(Multimap<String, Object> invalidParams) {
        try {
            JsonResponse response = new JsonResponse(400, Headers.of("Content-Type", "application/json"));
            String json = mapper.writeValueAsString(Map.of(
                    "type", "about:blank",
                    "title", "Malformed request",
                    "status", 400,
                    "invalid-params", invalidParams));
            response.setBody(json);
            return response;
        } catch (JsonProcessingException e) {
            return serverError(e);
        }

    }

    public static JsonResponse unauthenticated(String detail) {
        try {
            JsonResponse response = new JsonResponse(401, Headers.of("Content-Type", "application/json"));
            String json = mapper.writeValueAsString(Map.of(
                    "type", "about:blank",
                    "title", "Unauthenticated",
                    "status", 401,
                    "detail", detail));
            response.setBody(json);
            return response;
        } catch (JsonProcessingException e) {
            return serverError(e);
        }
    }

    public static JsonResponse serverError(Exception e) {
        JsonResponse response = new JsonResponse(500, Headers.of("Content-Type", "application/json"));
        response.setBody("{\"errors\": [{message: " + e.getMessage() + "\"}");
        return response;
    }
}
