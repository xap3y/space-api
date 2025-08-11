package me.xap3y.space.api.exception;

public class EmailVerifyCodeExpired extends RuntimeException {
    public EmailVerifyCodeExpired(String message) {
        super(message);
    }

    public EmailVerifyCodeExpired() {
        super("Email verification code has expired. Please request a new one.");
    }
}
