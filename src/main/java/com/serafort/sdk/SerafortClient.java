package com.serafort.sdk;

import com.serafort.sdk.b2b.B2BModule;
import com.serafort.sdk.b2b.UserContext;
import com.serafort.sdk.m2m.M2MModule;

import java.util.List;

public class SerafortClient {
    private final M2MModule m2m;
    private final B2BModule b2b;

    public SerafortClient(SerafortConfig config) {
        this.m2m = new M2MModule(config);
        this.b2b = new B2BModule(config);
    }

    public M2MModule m2m() {
        return m2m;
    }

    public B2BModule b2b() {
        return b2b;
    }

    public String getAccessToken(List<String> scopes) {
        return m2m.getAccessToken(scopes);
    }

    public UserContext validateToken(String token) {
        return b2b.validateToken(token);
    }

    public boolean hasPermission(UserContext user, String requiredPermission) {
        return b2b.hasPermission(user, requiredPermission);
    }

    public boolean hasRole(UserContext user, String requiredRole) {
        return b2b.hasRole(user, requiredRole);
    }

    public String getLoginUrl(String tenantId, String redirectUri) {
        return b2b.getLoginUrl(tenantId, redirectUri);
    }
}
