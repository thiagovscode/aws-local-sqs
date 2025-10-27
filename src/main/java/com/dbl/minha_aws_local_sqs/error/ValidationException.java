package com.dbl.minha_aws_local_sqs.error;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) { super(message); }
}
