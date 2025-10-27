package com.dbl.minha_aws_local_sqs.service;

import com.dbl.minha_aws_local_sqs.error.ExternalServiceException;
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

    // fornecido por WebClientConfig
    private final WebClient apiClient;

    public ProcessResponse callProcess(String token, ProcessRequest req) {
        return apiClient.post()
                .uri("/process")
                .headers(h -> h.setBearerAuth(token))
                .bodyValue(req)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp ->
                        resp.bodyToMono(String.class).defaultIfEmpty("<empty>")
                                .flatMap(b -> {
                                    log.warn("4xx from /process body={}", b);
                                    return Mono.error(new ExternalServiceException("4xx from process: " + b));
                                })
                )
                .onStatus(HttpStatusCode::is5xxServerError, resp ->
                        resp.bodyToMono(String.class).defaultIfEmpty("<empty>")
                                .flatMap(b -> {
                                    log.error("5xx from /process body={}", b);
                                    return Mono.error(new ExternalServiceException("5xx from process: " + b));
                                })
                )
                .bodyToMono(ProcessResponse.class)
                .timeout(Duration.ofSeconds(8))   // -> lança java.util.concurrent.TimeoutException
                .block();
    }

    // DTOs do contrato externo
    public static record ProcessRequest(String id, String payload, String metadata, String placaOuChassi) {}
    public static record ProcessResponse(String status, String details) {}
}
