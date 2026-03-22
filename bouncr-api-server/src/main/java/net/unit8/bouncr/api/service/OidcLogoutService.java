package net.unit8.bouncr.api.service;

import net.unit8.bouncr.api.repository.OidcApplicationRepository;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OidcLogoutService {
    private static final Logger LOG = LoggerFactory.getLogger(OidcLogoutService.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NORMAL)
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

        List<String> frontchannelUrls = new ArrayList<>();
        int attempted = 0;
        int succeeded = 0;
        int failed = 0;

        for (OidcApplication app : apps) {
            if (app.frontchannelLogoutUri() != null) {
                frontchannelUrls.add(app.frontchannelLogoutUri().toString());
            }
            if (app.backchannelLogoutUri() == null) {
                continue;
            }

            attempted++;
            if (sendBackchannelLogout(app, subject)) {
                succeeded++;
            } else {
                failed++;
            }
        }

        return new LogoutResult(frontchannelUrls, new BackchannelLogoutSummary(attempted, succeeded, failed));
    }

    private boolean sendBackchannelLogout(OidcApplication app, String subject) {
        if (app.clientId() == null || app.privateKey() == null || app.publicKey() == null) {
            LOG.warn("Skip back-channel logout for app {} due to missing key/client data", app.name());
            return false;
        }

        try {
            String logoutToken = createLogoutToken(app, subject);
            String body = "logout_token=" + URLEncoder.encode(logoutToken, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(app.backchannelLogoutUri().toURI())
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return true;
            }

            LOG.warn("Back-channel logout failed for app {} (status={} body={})",
                    app.name(), response.statusCode(), response.body());
            return false;
        } catch (Exception e) {
            LOG.warn("Back-channel logout request failed for app {}: {}", app.name(), e.getMessage(), e);
            return false;
        }
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
