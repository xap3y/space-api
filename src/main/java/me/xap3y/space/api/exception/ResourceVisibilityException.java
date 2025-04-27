package me.xap3y.space.api.exception;

public class ResourceVisibilityException extends RuntimeException {
    public ResourceVisibilityException(String message) {
        super(message);
    }

    public ResourceVisibilityException() {
        super("Resource is not publicly visible");
    }
}
