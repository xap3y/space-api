package me.xap3y.space.api.exception;

public class ResourceAccessForbiddenException extends RuntimeException {
    public ResourceAccessForbiddenException(String message) {
        super(message);
    }

    public ResourceAccessForbiddenException() {
        super("Access to this resource is forbidden");
    }
}
