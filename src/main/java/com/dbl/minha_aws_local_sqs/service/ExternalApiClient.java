package com.dbl.minha_aws_local_sqs.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ExternalApiClient {

    private final WebClient authClient;
    private final WebClient apiClient;

    public ExternalApiClient(@Qualifier("authClient") WebClient authClient,
                             @Qualifier("apiClient") WebClient apiClient) {
        this.authClient = authClient;
        this.apiClient  = apiClient;
    }

    public String fetchToken(String clientId, String clientSecret, String scope) {
        // usar authClient para /oauth/token
        // return authClient.post()...
        return "token-mock";
    }

    public ProcessResponse callProcess(String token, ProcessRequest req) {
        // usar apiClient para /process
        // return apiClient.post()...
        return new ProcessResponse("OK","processado");
    }

    public static record ProcessRequest(String id, String payload, String metadata, String placaOuChassi) {}
    public static record ProcessResponse(String status, String details) {}
}
