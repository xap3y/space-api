package me.xap3y.space.api.exception;

public class InvalidApiKeyException extends RuntimeException {
    public InvalidApiKeyException(String message) {
        super(message);
    }

    public InvalidApiKeyException() {
        super("Invalid API key");
    }
}
