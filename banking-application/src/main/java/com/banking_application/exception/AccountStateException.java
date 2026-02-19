package com.banking_application.exception;

public class AccountStateException extends RuntimeException {

    private final String accountNumber;

    public AccountStateException(String accountNumber, String message) {
        super(message);
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
