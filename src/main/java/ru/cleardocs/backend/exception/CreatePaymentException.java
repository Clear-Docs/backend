package ru.cleardocs.backend.exception;

public class CreatePaymentException extends RuntimeException {
    public CreatePaymentException(String message) {
        super(message);
    }
}
