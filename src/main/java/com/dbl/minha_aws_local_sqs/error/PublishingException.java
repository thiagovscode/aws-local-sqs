package com.dbl.minha_aws_local_sqs.error;

public class PublishingException extends RuntimeException {
    public PublishingException(String message) { super(message); }
    public PublishingException(String message, Throwable cause) { super(message, cause); }
}
