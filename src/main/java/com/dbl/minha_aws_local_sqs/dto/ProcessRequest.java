package com.dbl.minha_aws_local_sqs.dto;

public record ProcessRequest(
        String id,
        String payload,
        String metadata,
        String placaOuChassi
) {}
