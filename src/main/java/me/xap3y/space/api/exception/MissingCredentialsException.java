package me.xap3y.space.api.exception;

public class MissingCredentialsException extends RuntimeException {

    public MissingCredentialsException(String message) {
        super(message);
    }

    public MissingCredentialsException() {
        super("Missing credentials");
    }
}
