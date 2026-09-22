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

## Contributing

### Requirements

- Java 17+
- Maven 3.8+

### Git hooks

This repo ships a portable pre-commit hook under `.githooks/pre-commit` that runs a local `mvn compile` sanity check before every commit. It is **not** installed automatically — enable it once per clone with:

```bash
git config core.hooksPath .githooks
```

There is no Husky setup here: Husky is an npm-ecosystem tool that hooks into `package.json`/`node_modules`, and this is a pure Maven module with no Node.js tooling involved. A plain POSIX shell script wired through `core.hooksPath` is the idiomatic equivalent for a Maven/JVM repo and keeps the module dependency-free.

### CI

Every push and pull request against `main` runs `mvn -B verify` on Java 17 (Temurin) via GitHub Actions (`.github/workflows/ci.yml`).
