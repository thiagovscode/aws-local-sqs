package com.dbl.minha_aws_local_sqs.dto;

import com.dbl.minha_aws_local_sqs.service.ExternalApiClient.ProcessResponse;

public record SaidaProcessamento(
        String id,
        String placaOuChassi,
        String produto,
        String status,
        String detalhes
) {
    public static SaidaProcessamento of(SolicitacaoVeicular in, ProcessResponse resp) {
        return new SaidaProcessamento(
                in.id(), in.placaOuChassi(), in.produto(), resp.status(), resp.details()
        );
    }
}

