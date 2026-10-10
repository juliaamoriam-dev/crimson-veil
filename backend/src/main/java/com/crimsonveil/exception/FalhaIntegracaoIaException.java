package com.crimsonveil.exception;

public class FalhaIntegracaoIaException extends RuntimeException {
    public FalhaIntegracaoIaException(String message) {
        super(message);
    }

    public FalhaIntegracaoIaException(String message, Throwable cause) {
        super(message, cause);
    }
}
