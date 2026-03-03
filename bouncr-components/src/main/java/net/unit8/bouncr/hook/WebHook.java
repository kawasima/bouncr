package net.unit8.bouncr.hook;

import tools.jackson.databind.json.JsonMapper;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import okhttp3.*;

import java.net.SocketTimeoutException;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class WebHook<T> implements Hook<T> {
    private static final OkHttpClient client = new OkHttpClient();
    private static final JsonMapper mapper = JsonMapper.builder().build();
    private static final MediaType JSON = MediaType.parse("application/json");
    private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

    private RetryPolicy<Response> idempotent = RetryPolicy.<Response>builder()
            .withBackoff(3, 10, ChronoUnit.SECONDS)
            .handle(SocketTimeoutException.class)
            .handleResultIf(res -> res.code() >= 500)
            .build();

    private RetryPolicy<Response> notIdempotent = RetryPolicy.<Response>builder()
            .withBackoff(3, 10, ChronoUnit.SECONDS)
            .handleResultIf(res -> res.code() >= 500)
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
        RetryPolicy<Response> retryPolicy = method.equalsIgnoreCase("get") ? idempotent : notIdempotent;
        Failsafe.with(retryPolicy)
                .with(executor)
                .onSuccess(response -> {

                })
                .get(() -> {
                    RequestBody body = RequestBody.create(mapper.writeValueAsString(object), JSON);
                    Request.Builder requestBuilder;
                    requestBuilder = new Request.Builder()
                            .url(url)
                            .header("content-type", "application/json")
                            .method(method, body);
                    if (headers != null) {
                        headers.forEach(requestBuilder::addHeader);
                    }
                    return client.newCall(requestBuilder.build()).execute();
                });
    }
}
