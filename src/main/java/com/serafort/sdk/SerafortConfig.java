package com.serafort.sdk;

import java.time.Duration;

public class SerafortConfig {
    private final String endpoint;
    private final String clientId;
    private final String clientSecret;
    private final String privateKey;
    private final Duration timeout;

    private SerafortConfig(Builder builder) {
        this.endpoint = builder.endpoint;
        this.clientId = builder.clientId;
        this.clientSecret = builder.clientSecret;
        this.privateKey = builder.privateKey;
        this.timeout = builder.timeout != null ? builder.timeout : Duration.ofSeconds(10);
    }

    public static Builder builder(String endpoint) {
        return new Builder(endpoint);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public static class Builder {
        private final String endpoint;
        private String clientId;
        private String clientSecret;
        private String privateKey;
        private Duration timeout;

        public Builder(String endpoint) {
            this.endpoint = endpoint != null ? endpoint.replaceAll("/+$", "") : "";
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder clientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        public Builder privateKey(String privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public SerafortConfig build() {
            return new SerafortConfig(this);
        }
    }
}
