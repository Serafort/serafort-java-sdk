package com.serafort.sdk.b2b;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UserContext {
    private final String userId;
    private final String tenantId;
    private final String email;
    private final List<String> roles;
    private final List<String> permissions;
    private final Map<String, Object> claims;

    public UserContext(String userId, String tenantId, String email, List<String> roles, List<String> permissions, Map<String, Object> claims) {
        this.userId = userId != null ? userId : "";
        this.tenantId = tenantId != null ? tenantId : "";
        this.email = email;
        this.roles = roles != null ? Collections.unmodifiableList(roles) : Collections.emptyList();
        this.permissions = permissions != null ? Collections.unmodifiableList(permissions) : Collections.emptyList();
        this.claims = claims != null ? Collections.unmodifiableMap(claims) : Collections.emptyMap();
    }

    public String getUserId() {
        return userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public Map<String, Object> getClaims() {
        return claims;
    }
}
