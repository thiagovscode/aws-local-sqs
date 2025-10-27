package com.dbl.minha_aws_local_sqs.service;

import com.dbl.minha_aws_local_sqs.dto.ProcessRequest;
import com.dbl.minha_aws_local_sqs.dto.ProcessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalApiClient {

    private final WebClient apiClient; // definido em WebClientConfig

    /**
     * Chama /process depois de já ter o token.
     * - Para qualquer status HTTP, retorna um ProcessResponse
     *   preenchendo httpStatus e details (corpo como String).
     * - Se 200, joga o corpo também em 'payload' para frente.
     */
    public ProcessResponse callProcess(String bearerToken, ProcessRequest req) {
        return apiClient.post()
                .uri("/process")
                .headers(h -> h.setBearerAuth(bearerToken))
                .bodyValue(req)
                .exchangeToMono(resp -> {
                    int sc = resp.statusCode().value();
                    // lê corpo como String e mapeia para nosso DTO
                    return resp.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(body -> {
                                if (HttpStatusCode.valueOf(sc).is2xxSuccessful()) {
                                    // sucesso → payload preenchido, details "OK"
                                    return new ProcessResponse(sc, "OK", body);
                                } else {
                                    // erro → details = corpo, payload null
                                    return new ProcessResponse(sc, body, null);
                                }
                            });
                })
                .timeout(Duration.ofSeconds(8))
                .block();
    }
}
