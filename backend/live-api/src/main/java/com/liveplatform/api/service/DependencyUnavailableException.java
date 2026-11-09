package com.liveplatform.api.service;

public class DependencyUnavailableException extends RuntimeException {
    public DependencyUnavailableException(Throwable cause) {
        super("User provider probe failed", cause);
    }
}
