package com.dbl.minha_aws_local_sqs.error;

public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) { super(message); }
    public ExternalServiceException(String message, Throwable cause) { super(message, cause); }
}
