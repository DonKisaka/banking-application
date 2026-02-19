package com.banking_application.exception;

public class InsufficientFundsException extends RuntimeException {

    private final String accountNumber;

    public InsufficientFundsException(String accountNumber) {
        super("Insufficient funds in account: " + accountNumber);
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
