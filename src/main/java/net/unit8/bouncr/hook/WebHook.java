package net.unit8.bouncr.hook;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import okhttp3.*;

import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class WebHook<T> implements Hook<T> {
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final MediaType JSON = MediaType.parse("application/json");
    private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

    private RetryPolicy idempotent = new RetryPolicy()
            .withBackoff(3, 10, TimeUnit.SECONDS)
            .retryOn(SocketTimeoutException.class)
            .retryIf(res -> Stream.of(res)
                    .map(Response.class::cast)
                    .map(Response::code)
                    .anyMatch(code -> code >= 500));

    private RetryPolicy notIdempotent = new RetryPolicy()
            .withBackoff(3, 10, TimeUnit.SECONDS)
            .retryIf(res -> Stream.of(res)
                    .map(Response.class::cast)
                    .map(Response::code)
                    .anyMatch(code -> code >= 500));

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
        RetryPolicy retryPolicy = method.equalsIgnoreCase("get") ? idempotent : notIdempotent;
        Failsafe.with(retryPolicy)
                .with(executor)
                .onSuccess(response -> {

                })
                .get(() -> {
                    RequestBody body = RequestBody.create(JSON, mapper.writeValueAsString(object));
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
