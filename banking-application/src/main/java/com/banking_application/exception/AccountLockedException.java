package com.banking_application.exception;

public class AccountLockedException extends RuntimeException {

    private final String username;
    private final String lockedUntil;

    public AccountLockedException(String username, String lockedUntil) {
        super(String.format("Account for user '%s' is locked until %s", username, lockedUntil));
        this.username = username;
        this.lockedUntil = lockedUntil;
    }

    public String getUsername() {
        return username;
    }

    public String getLockedUntil() {
        return lockedUntil;
    }
}
