package com.dbl.minha_aws_local_sqs.service;

import com.dbl.minha_aws_local_sqs.dto.SaidaProcessamento;
import com.dbl.minha_aws_local_sqs.dto.SolicitacaoVeicular;
import com.dbl.minha_aws_local_sqs.error.ExternalServiceException;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessingService {

    private final ExternalApiClient external;

    /**
     * Varre a cadeia de causas procurando timeouts típicos.
     */
    private static boolean isTimeout(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof TimeoutException) return true;                 // do .timeout(...)
            if (c instanceof ReadTimeoutException) return true;             // Netty
            if (c instanceof java.net.SocketTimeoutException) return true;  // socket (se aparecer)
        }
        return false;
    }

    public SaidaProcessamento process(SolicitacaoVeicular msg) {
        try {
            String token = "token-mock";
            var req = new ExternalApiClient.ProcessRequest(
                    msg.id(), msg.produto(), "from-sqs", msg.placaOuChassi()
            );
            var resp = external.callProcess(token, req);
            return SaidaProcessamento.of(msg, resp);

        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString();
            log.error("HTTP {} from external. corrId={} body={}",
                    e.getRawStatusCode(), msg.id(), body);
            throw new ExternalServiceException("HTTP " + e.getRawStatusCode(), e);

        } catch (Exception e) {
            if (isTimeout(e)) {
                log.error("Timeout calling external. corrId={}", msg.id());
                throw new ExternalServiceException("Timeout calling external", e);
            }
            log.error("Unexpected processing error corrId={}", msg.id(), e);
            throw new ExternalServiceException("Unexpected external error", e);
        }
    }
}
