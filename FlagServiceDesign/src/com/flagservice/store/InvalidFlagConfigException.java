package com.flagservice.store;

public class InvalidFlagConfigException extends RuntimeException {
    public InvalidFlagConfigException(String message) {
        super(message);
    }

    public InvalidFlagConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
