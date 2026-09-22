package com.serafort.sdk.b2b;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serafort.sdk.SerafortConfig;
import com.serafort.sdk.errors.AuthenticationException;
import com.serafort.sdk.errors.SerafortException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class B2BModule {
    private final SerafortConfig config;
    private final ObjectMapper mapper = new ObjectMapper();

    public B2BModule(SerafortConfig config) {
        this.config = config;
    }

    public UserContext validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new AuthenticationException("Token must not be empty");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new AuthenticationException("Invalid JWT format: expected 3 parts");
        }

        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payloadNode = mapper.readTree(payloadBytes);

            // Expiration validation
            if (payloadNode.has("exp")) {
                long exp = payloadNode.get("exp").asLong();
                if (Instant.now().getEpochSecond() > exp + 60) { // 60s clock tolerance
                    throw new AuthenticationException("Token has expired");
                }
            }

            // Issuer validation
            String expectedIssuer = config.getEndpoint();
            if (payloadNode.has("iss") && expectedIssuer != null && !expectedIssuer.isEmpty()) {
                String iss = payloadNode.get("iss").asText();
                if (!expectedIssuer.equals(iss)) {
                    throw new AuthenticationException("Invalid token issuer: expected " + expectedIssuer + ", got " + iss);
                }
            }

            String userId = payloadNode.has("sub") ? payloadNode.get("sub").asText() :
                    (payloadNode.has("user_id") ? payloadNode.get("user_id").asText() : "");

            String tenantId = payloadNode.has("tenant_id") ? payloadNode.get("tenant_id").asText() :
                    (payloadNode.has("org_id") ? payloadNode.get("org_id").asText() : "");

            String email = payloadNode.has("email") ? payloadNode.get("email").asText() : null;

            List<String> roles = new ArrayList<>();
            if (payloadNode.has("roles") && payloadNode.get("roles").isArray()) {
                for (JsonNode r : payloadNode.get("roles")) {
                    roles.add(r.asText());
                }
            } else if (payloadNode.has("role")) {
                roles.add(payloadNode.get("role").asText());
            }

            List<String> permissions = new ArrayList<>();
            if (payloadNode.has("permissions") && payloadNode.get("permissions").isArray()) {
                for (JsonNode p : payloadNode.get("permissions")) {
                    permissions.add(p.asText());
                }
            } else if (payloadNode.has("scope")) {
                String[] scopes = payloadNode.get("scope").asText().split("\\s+");
                for (String s : scopes) {
                    if (!s.isEmpty()) {
                        permissions.add(s);
                    }
                }
            }

            Map<String, Object> claims = mapper.convertValue(payloadNode, new TypeReference<Map<String, Object>>() {});

            return new UserContext(userId, tenantId, email, roles, permissions, claims);

        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthenticationException("Failed to decode token claims: " + e.getMessage());
        }
    }

    public boolean hasPermission(UserContext user, String requiredPermission) {
        if (user == null || user.getPermissions().isEmpty()) {
            return false;
        }

        for (String perm : user.getPermissions()) {
            if ("*".equals(perm) || perm.equals(requiredPermission)) {
                return true;
            }
            if (perm.endsWith(":*")) {
                String prefix = perm.substring(0, perm.length() - 2);
                if (requiredPermission.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasRole(UserContext user, String requiredRole) {
        return user != null && user.getRoles().contains(requiredRole);
    }

    public String getLoginUrl(String tenantId, String redirectUri) {
        return config.getEndpoint() + "/api/auth/sso/login?tenant_id="
                + URLEncoder.encode(tenantId, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
    }
}
