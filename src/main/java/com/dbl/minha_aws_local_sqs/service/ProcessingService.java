package com.dbl.minha_aws_local_sqs.service;

import com.dbl.minha_aws_local_sqs.dto.ProcessRequest;
import com.dbl.minha_aws_local_sqs.dto.ProcessResponse;
import com.dbl.minha_aws_local_sqs.dto.SaidaProcessamento;
import com.dbl.minha_aws_local_sqs.dto.SolicitacaoVeicular;
import com.dbl.minha_aws_local_sqs.error.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessingService {

    private final ExternalApiClient external;

    public SaidaProcessamento process(SolicitacaoVeicular msg) {
        long t0 = System.currentTimeMillis();
        try {
            // Em ambiente local estamos usando token mock.
            String token = "token-mock";

            ProcessRequest req = new ProcessRequest(
                    msg.id(), msg.produto(), "from-sqs", msg.placaOuChassi()
            );

            ProcessResponse resp = external.callProcess(token, req);

            int sc = resp.httpStatus();
            switch (sc) {
                case 200 -> {
                    log.debug("OK corrId={} took={}ms", msg.id(), System.currentTimeMillis() - t0);
                    return SaidaProcessamento.success(msg, resp.details(), resp.payload());
                }
                case 400 -> {
                    log.info("Regra negativa corrId={} details={}", msg.id(), resp.details());
                    return SaidaProcessamento.failure(msg, "Falha de consulta: " + resp.details());
                }
                case 404 -> {
                    log.info("Nao encontrado corrId={}", msg.id());
                    return SaidaProcessamento.notFound(msg, "Dados não encontrados");
                }
                default -> {
                    if (sc >= 500) {
                        log.error("Erro externo corrId={} status={} details={}", msg.id(), sc, resp.details());
                        return SaidaProcessamento.error(msg, "Erro interno: " + resp.details());
                    }
                    log.warn("Status inesperado corrId={} status={}", msg.id(), sc);
                    return SaidaProcessamento.error(msg, "Estado desconhecido (status=" + sc + ")");
                }
            }

        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString();
            log.error("HTTP {} exception corrId={} body={}", e.getRawStatusCode(), msg.id(), body);
            throw new ExternalServiceException("HTTP " + e.getRawStatusCode(), e);

        } catch (Exception e) {
            log.error("Unexpected processing error corrId={}", msg.id(), e);
            throw new ExternalServiceException("Unexpected external error", e);
        }
    }
}
