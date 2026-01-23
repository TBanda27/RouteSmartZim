package com.routesmart.exception;

public class InvalidInputException extends RuntimeException {

    private final String field;
    private final Object rejectedValue;

    public InvalidInputException(String message) {
        super(message);
        this.field = null;
        this.rejectedValue = null;
    }

    public InvalidInputException(String message, String field) {
        super(message);
        this.field = field;
        this.rejectedValue = null;
    }

    public InvalidInputException(String message, String field, Object rejectedValue) {
        super(message);
        this.field = field;
        this.rejectedValue = rejectedValue;
    }

    public String getField() {
        return field;
    }

    public Object getRejectedValue() {
        return rejectedValue;
    }
}
