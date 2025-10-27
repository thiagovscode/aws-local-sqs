package com.dbl.minha_aws_local_sqs.dto;

public record SaidaProcessamento(
        String correlationId,
        int status,
        String message,
        Object data
) {
    public static SaidaProcessamento success(SolicitacaoVeicular msg, String message, Object data) {
        return new SaidaProcessamento(msg.id(), 200, message, data);
    }
    public static SaidaProcessamento failure(SolicitacaoVeicular msg, String message) {
        return new SaidaProcessamento(msg.id(), 400, message, null);
    }
    public static SaidaProcessamento notFound(SolicitacaoVeicular msg, String message) {
        return new SaidaProcessamento(msg.id(), 404, message, null);
    }
    public static SaidaProcessamento error(SolicitacaoVeicular msg, String message) {
        return new SaidaProcessamento(msg.id(), 500, message, null);
    }
}
