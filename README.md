# Serafort Java SDK (`com.serafort:serafort-sdk`)

Official Java client library for the Serafort identity platform. High-performance, concurrency-safe Machine Identity (M2M) caching and local B2B JWT validation for Java 17+ and Spring Boot.

## Installation

### Maven

```xml
<dependency>
    <groupId>com.serafort</groupId>
    <artifactId>serafort-sdk</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.serafort:serafort-sdk:0.1.0'
```

## Quickstart

```java
import com.serafort.sdk.SerafortClient;
import com.serafort.sdk.SerafortConfig;
import com.serafort.sdk.b2b.UserContext;
import java.util.List;

public class App {
    public static void main(String[] args) {
        SerafortConfig config = SerafortConfig.builder("https://auth.acme.com")
                .clientId("my-client-id")
                .clientSecret("my-client-secret")
                .build();

        SerafortClient client = new SerafortClient(config);

        // 1. Retrieve M2M access token (cached, proactive 5-min refresh)
        String token = client.getAccessToken(List.of("read:users"));
        System.out.println("M2M Token: " + token);

        // 2. Validate user token locally
        UserContext user = client.validateToken(token);
        System.out.println("User: " + user.getUserId() + " in tenant: " + user.getTenantId());

        // 3. Check RBAC permissions (wildcards supported)
        if (client.hasPermission(user, "org:write")) {
            System.out.println("Access granted!");
        }
    }
}
```
