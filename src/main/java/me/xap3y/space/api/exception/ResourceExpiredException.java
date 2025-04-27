package me.xap3y.space.api.exception;

public class ResourceExpiredException extends RuntimeException {
    public ResourceExpiredException(String message) {
        super(message);
    }

    public ResourceExpiredException() {
        super("Resource expired");
    }
}
