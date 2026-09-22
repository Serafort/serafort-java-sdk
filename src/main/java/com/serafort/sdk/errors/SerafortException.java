package com.serafort.sdk.errors;

public class SerafortException extends RuntimeException {
    private final String code;
    private final int status;

    public SerafortException(String message, String code, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public SerafortException(String message, Throwable cause) {
        super(message, cause);
        this.code = "INTERNAL_ERROR";
        this.status = 500;
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }
}
