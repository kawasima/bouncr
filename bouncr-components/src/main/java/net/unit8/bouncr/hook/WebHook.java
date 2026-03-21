package net.unit8.bouncr.hook;

import tools.jackson.databind.json.JsonMapper;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class WebHook<T> implements Hook<T> {
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private static final JsonMapper mapper = JsonMapper.builder().build();
    private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

    private RetryPolicy<HttpResponse<String>> idempotent = RetryPolicy.<HttpResponse<String>>builder()
            .withBackoff(3, 10, ChronoUnit.SECONDS)
            .handle(HttpTimeoutException.class)
            .handle(HttpConnectTimeoutException.class)
            .handle(IOException.class)
            .handleResultIf(res -> res.statusCode() >= 500)
            .build();

    private RetryPolicy<HttpResponse<String>> notIdempotent = RetryPolicy.<HttpResponse<String>>builder()
            .withBackoff(3, 10, ChronoUnit.SECONDS)
            .handle(HttpTimeoutException.class)
            .handle(HttpConnectTimeoutException.class)
            .handle(IOException.class)
            .handleResultIf(res -> res.statusCode() >= 500)
            .build();

    private final String method;
    private final String url;
    private Map<String, String> headers = null;

    public WebHook(String url, String method) {
        this.url = url;
        this.method = method;
    }

    public WebHook(String url, String method, Map<String, String> headers) {
        this.url = url;
        this.method = method;
        this.headers = headers;
    }

    @Override
    public void run(T object) {
        RetryPolicy<HttpResponse<String>> retryPolicy = method.equalsIgnoreCase("get") ? idempotent : notIdempotent;
        Failsafe.with(retryPolicy)
                .with(executor)
                .onSuccess(response -> {

                })
                .get(() -> {
                    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(Duration.ofSeconds(10))
                            .header("content-type", "application/json");
                    if (headers != null) {
                        headers.forEach(requestBuilder::header);
                    }
                    HttpRequest request;
                    if (method.equalsIgnoreCase("get")) {
                        request = requestBuilder.GET().build();
                    } else {
                        String payload = mapper.writeValueAsString(object);
                        request = requestBuilder
                                .method(method, HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                                .build();
                    }
                    try {
                        return client.send(request, HttpResponse.BodyHandlers.ofString());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Interrupted while sending webhook", e);
                    }
                });
    }
}
