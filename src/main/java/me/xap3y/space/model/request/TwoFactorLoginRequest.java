package me.xap3y.space.model.request;

public record TwoFactorLoginRequest(String challengeToken, String code) {}
