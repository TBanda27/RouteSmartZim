package com.routesmart.exception;

public class GoogleMapsApiException extends RuntimeException {

    private final String errorCode;

    public GoogleMapsApiException(String message) {
        super(message);
        this.errorCode = null;
    }

    public GoogleMapsApiException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public GoogleMapsApiException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
