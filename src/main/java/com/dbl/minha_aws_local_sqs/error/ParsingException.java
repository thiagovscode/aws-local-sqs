package com.dbl.minha_aws_local_sqs.error;

public class ParsingException extends RuntimeException {
    public ParsingException(String message) { super(message); }
    public ParsingException(String message, Throwable cause) { super(message, cause); }
}
