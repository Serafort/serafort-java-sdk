package com.serafort.sdk.errors;

public class AuthenticationException extends SerafortException {
    public AuthenticationException(String message) {
        super(message, "AUTHENTICATION_ERROR", 401);
    }

    public AuthenticationException(String message, String code) {
        super(message, code, 401);
    }
}
