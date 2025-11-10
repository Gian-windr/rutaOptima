package com.customer.rutaOptima.config.exception;

/**
 * Excepción para errores de negocio (HTTP 422 - Unprocessable Entity)
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
