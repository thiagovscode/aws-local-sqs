package com.dbl.minha_aws_local_sqs.dto;


public record ProcessResponse(
        String status,
        String details
) {}
