package me.xap3y.space.api.exception;

public class InvalidUniqueIdException extends RuntimeException {
    public InvalidUniqueIdException(String message) {
        super(message);
    }

    public InvalidUniqueIdException() {
        super("Invalid unique id provided");
    }
}
