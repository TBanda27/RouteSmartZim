package com.routesmart.exception;

public class OptimizerServiceException extends RuntimeException {

    private final boolean serviceUnavailable;

    public OptimizerServiceException(String message) {
        super(message);
        this.serviceUnavailable = false;
    }

    public OptimizerServiceException(String message, boolean serviceUnavailable) {
        super(message);
        this.serviceUnavailable = serviceUnavailable;
    }

    public OptimizerServiceException(String message, Throwable cause) {
        super(message, cause);
        this.serviceUnavailable = true;
    }

    public boolean isServiceUnavailable() {
        return serviceUnavailable;
    }
}
