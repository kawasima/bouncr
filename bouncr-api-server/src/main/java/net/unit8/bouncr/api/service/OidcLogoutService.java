package net.unit8.bouncr.api.service;

import net.unit8.bouncr.api.repository.OidcApplicationRepository;
import net.unit8.bouncr.api.util.LogoutUriPolicy;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.data.OidcApplication;
import net.unit8.bouncr.sign.RsaJwtSigner;
import net.unit8.bouncr.util.KeyEncryptor;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class OidcLogoutService {
    private static final Logger LOG = LoggerFactory.getLogger(OidcLogoutService.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration OVERALL_BACKCHANNEL_TIMEOUT = Duration.ofSeconds(8);
    private static final int MAX_BACKCHANNEL_TARGETS = 20;
    private static final int BACKCHANNEL_CONCURRENCY = 4;
    private static final int RESPONSE_BODY_LOG_LIMIT = 200;
    private static final ExecutorService BACKCHANNEL_EXECUTOR = Executors.newFixedThreadPool(
            BACKCHANNEL_CONCURRENCY,
            runnable -> {
                Thread thread = new Thread(runnable, "oidc-backchannel-logout");
                thread.setDaemon(true);
                return thread;
            });
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .executor(BACKCHANNEL_EXECUTOR)
            .build();

    private final BouncrConfiguration config;

    public OidcLogoutService(BouncrConfiguration config) {
        this.config = config;
    }

    public record BackchannelLogoutSummary(int attempted, int succeeded, int failed) {}
    public record LogoutResult(List<String> frontchannelLogoutUrls, BackchannelLogoutSummary backchannelLogout) {}

    public LogoutResult propagateSignOut(String subject, DSLContext dsl) {
        OidcApplicationRepository repo = new OidcApplicationRepository(dsl);
        List<OidcApplication> apps = repo.listAll();
        List<String> frontchannelUrls = collectFrontchannelUrls(apps);

        if (subject == null || subject.isBlank()) {
            LOG.warn("Skip back-channel logout propagation because subject is unresolved");
            return new LogoutResult(frontchannelUrls, new BackchannelLogoutSummary(0, 0, 0));
        }

        List<OidcApplication> backchannelTargets = collectBackchannelTargets(apps);
        BackchannelLogoutSummary summary = dispatchBackchannelLogouts(backchannelTargets, subject);
        return new LogoutResult(frontchannelUrls, summary);
    }

    private List<String> collectFrontchannelUrls(List<OidcApplication> apps) {
        List<String> frontchannelUrls = new ArrayList<>();
        for (OidcApplication app : apps) {
            if (app.frontchannelLogoutUri() != null) {
                frontchannelUrls.add(app.frontchannelLogoutUri().toString());
            }
        }
        return frontchannelUrls;
    }

    private List<OidcApplication> collectBackchannelTargets(List<OidcApplication> apps) {
        List<OidcApplication> targets = apps.stream()
                .filter(app -> app.backchannelLogoutUri() != null)
                .limit(MAX_BACKCHANNEL_TARGETS)
                .toList();
        long allBackchannelTargets = apps.stream().filter(app -> app.backchannelLogoutUri() != null).count();
        if (allBackchannelTargets > targets.size()) {
            LOG.warn("Back-channel logout targets are limited to {} out of {} applications",
                    targets.size(), allBackchannelTargets);
        }
        return targets;
    }

    private BackchannelLogoutSummary dispatchBackchannelLogouts(List<OidcApplication> targets, String subject) {
        if (targets.isEmpty()) {
            return new BackchannelLogoutSummary(0, 0, 0);
        }

        List<CompletableFuture<Boolean>> deliveries = targets.stream()
                .map(app -> sendBackchannelLogoutAsync(app, subject))
                .toList();

        CompletableFuture<Void> allDone = CompletableFuture
                .allOf(deliveries.toArray(new CompletableFuture[0]));
        try {
            allDone.get(OVERALL_BACKCHANNEL_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            LOG.warn("Back-channel logout delivery timed out after {} ms",
                    OVERALL_BACKCHANNEL_TIMEOUT.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Back-channel logout delivery interrupted: {}", e.getMessage(), e);
        } catch (ExecutionException e) {
            LOG.warn("Back-channel logout delivery interrupted: {}", unwrapException(e).getMessage(), e);
        }

        int succeeded = 0;
        int failed = 0;
        for (CompletableFuture<Boolean> delivery : deliveries) {
            if (!delivery.isDone()) {
                delivery.cancel(true);
                failed++;
                continue;
            }
            try {
                if (Boolean.TRUE.equals(delivery.getNow(Boolean.FALSE))) {
                    succeeded++;
                } else {
                    failed++;
                }
            } catch (CompletionException | CancellationException e) {
                failed++;
            }
        }
        return new BackchannelLogoutSummary(targets.size(), succeeded, failed);
    }

    private CompletableFuture<Boolean> sendBackchannelLogoutAsync(OidcApplication app, String subject) {
        if (app.clientId() == null || app.privateKey() == null || app.publicKey() == null) {
            LOG.warn("Skip back-channel logout for app {} due to missing key/client data", app.name());
            return CompletableFuture.completedFuture(false);
        }
        if (app.backchannelLogoutUri() == null) {
            return CompletableFuture.completedFuture(false);
        }

        try {
            var targetUri = app.backchannelLogoutUri().toURI();
            if (!LogoutUriPolicy.isAllowedBackchannelTarget(targetUri)) {
                LOG.warn("Skip back-channel logout for app {} due to disallowed target URI", app.name());
                return CompletableFuture.completedFuture(false);
            }
            String logoutToken = createLogoutToken(app, subject);
            String body = "logout_token=" + URLEncoder.encode(logoutToken, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(targetUri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            return true;
                        }
                        logBackchannelHttpFailure(app, response);
                        return false;
                    })
                    .exceptionally(e -> {
                        Throwable cause = unwrapException(e);
                        LOG.warn("Back-channel logout request failed for app {}: {}", app.name(), cause.getMessage());
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Back-channel logout request failed for app {}", app.name(), e);
                        }
                        return false;
                    });
        } catch (URISyntaxException e) {
            LOG.warn("Back-channel logout request preparation failed for app {}: {}", app.name(), e.getMessage());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Back-channel logout request preparation failed for app {}", app.name(), e);
            }
            return CompletableFuture.completedFuture(false);
        }
    }

    private void logBackchannelHttpFailure(OidcApplication app, HttpResponse<String> response) {
        LOG.warn("Back-channel logout failed for app {} (status={})", app.name(), response.statusCode());
        if (LOG.isDebugEnabled()) {
            String responseBody = response.body();
            String truncatedBody = responseBody != null && responseBody.length() > RESPONSE_BODY_LOG_LIMIT
                    ? responseBody.substring(0, RESPONSE_BODY_LOG_LIMIT) + "..."
                    : responseBody;
            LOG.debug("Back-channel logout response body for app {}: {}", app.name(), truncatedBody);
        }
    }

    private Throwable unwrapException(Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }

    String createLogoutToken(OidcApplication app, String subject) {
        KeyEncryptor encryptor = new KeyEncryptor(config.getKeyEncryptionKey(), config.getSecureRandom());
        byte[] privateKeyBytes = encryptor.decrypt(app.privateKey());
        try {
            long now = config.getClock().instant().getEpochSecond();
            String kid = RsaJwtSigner.deriveKid(app.publicKey());

            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put("iss", config.getIssuerBaseUrl());
            claims.put("aud", app.clientId());
            claims.put("iat", now);
            claims.put("jti", UUID.randomUUID().toString());
            claims.put("sub", subject);
            claims.put("events", Map.of("http://schemas.openid.net/event/backchannel-logout", Map.of()));
            return RsaJwtSigner.sign(claims, privateKeyBytes, kid);
        } finally {
            java.util.Arrays.fill(privateKeyBytes, (byte) 0);
        }
    }
}
