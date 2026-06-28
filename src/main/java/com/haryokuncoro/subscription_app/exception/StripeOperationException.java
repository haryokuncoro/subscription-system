package com.haryokuncoro.subscription_app.exception;

public class StripeOperationException extends RuntimeException {
    public StripeOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}