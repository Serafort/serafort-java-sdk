package com.serafort.sdk.m2m;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serafort.sdk.SerafortConfig;
import com.serafort.sdk.errors.AuthenticationException;
import com.serafort.sdk.errors.RateLimitException;
import com.serafort.sdk.errors.SerafortException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class CachedToken {
    final String token;
    final Instant expiresAt;

    CachedToken(String token, Instant expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
    }
}

public class M2MModule {
    private final SerafortConfig config;
    private final HttpClient httpClient;
    private final Map<String, CachedToken> cache = new ConcurrentHashMap<>();
    private final Map<String, Object> locks = new ConcurrentHashMap<>();
    private final Duration proactiveBuffer = Duration.ofMinutes(5);
    private final ObjectMapper mapper = new ObjectMapper();

    public M2MModule(SerafortConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.getTimeout())
                .build();
    }

    public String getAccessToken(List<String> scopes) {
        return getToken(scopes, false);
    }

    public String forceRefreshToken(List<String> scopes) {
        return getToken(scopes, true);
    }

    private String getToken(List<String> scopes, boolean force) {
        List<String> sortedScopes = scopes != null ? scopes : Collections.emptyList();
        String scopeKey = String.join(" ", sortedScopes);
        String cacheKey = (config.getClientId() != null ? config.getClientId() : "default") + ":" + scopeKey;

        if (!force) {
            CachedToken cached = cache.get(cacheKey);
            if (cached != null && Instant.now().plus(proactiveBuffer).isBefore(cached.expiresAt)) {
                return cached.token;
            }
        }

        // Synchronize on cacheKey lock to avoid thundering-herd on token refresh
        Object lock = locks.computeIfAbsent(cacheKey, k -> new Object());
        synchronized (lock) {
            if (!force) {
                CachedToken cached = cache.get(cacheKey);
                if (cached != null && Instant.now().plus(proactiveBuffer).isBefore(cached.expiresAt)) {
                    return cached.token;
                }
            }

            return fetchTokenWithRetry(cacheKey, scopeKey);
        }
    }

    private String fetchTokenWithRetry(String cacheKey, String scope) {
        int maxRetries = 3;
        long initialDelayMs = 500;
        long maxDelayMs = 5000;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            if (attempt > 0) {
                long delay = (long) (initialDelayMs * Math.pow(2, attempt - 1));
                long jitter = (long) (delay * (0.8 + Math.random() * 0.4));
                long sleepMs = Math.min(jitter, maxDelayMs);
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SerafortException("Token fetch thread interrupted", e);
                }
            }

            try {
                String tokenUrl = config.getEndpoint() + "/oauth/token";

                StringBuilder bodyBuilder = new StringBuilder();
                bodyBuilder.append("grant_type=client_credentials");
                if (config.getClientId() != null) {
                    bodyBuilder.append("&client_id=").append(URLEncoder.encode(config.getClientId(), StandardCharsets.UTF_8));
                }
                if (config.getClientSecret() != null) {
                    bodyBuilder.append("&client_secret=").append(URLEncoder.encode(config.getClientSecret(), StandardCharsets.UTF_8));
                }
                if (!scope.isEmpty()) {
                    bodyBuilder.append("&scope=").append(URLEncoder.encode(scope, StandardCharsets.UTF_8));
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(tokenUrl))
                        .timeout(config.getTimeout())
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(bodyBuilder.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 429) {
                    if (attempt == maxRetries) {
                        throw new RateLimitException("Rate limit exceeded on token endpoint", null);
                    }
                    continue;
                }

                if (response.statusCode() >= 500) {
                    if (attempt == maxRetries) {
                        throw new SerafortException("Server error during token fetch: " + response.statusCode(), "SERVER_ERROR", response.statusCode());
                    }
                    continue;
                }

                if (response.statusCode() != 200) {
                    throw new AuthenticationException("Token request failed (status " + response.statusCode() + "): " + response.body());
                }

                JsonNode root = mapper.readTree(response.body());
                String accessToken = root.path("access_token").asText(null);
                long expiresIn = root.path("expires_in").asLong(3600);

                if (accessToken == null || accessToken.isEmpty()) {
                    throw new AuthenticationException("Invalid token response: access_token missing");
                }

                Instant expiresAt = Instant.now().plusSeconds(expiresIn);
                cache.put(cacheKey, new CachedToken(accessToken, expiresAt));
                return accessToken;

            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                if (attempt == maxRetries) {
                    throw new SerafortException("Failed to fetch token after retries", e);
                }
            }
        }

        throw new SerafortException("Exceeded maximum retry attempts for token fetch", "MAX_RETRIES_EXCEEDED", 500);
    }
}
