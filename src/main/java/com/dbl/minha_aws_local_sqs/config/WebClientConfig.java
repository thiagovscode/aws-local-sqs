package com.dbl.minha_aws_local_sqs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;


@Configuration
public class WebClientConfig {

    @Bean("authClient")
    public WebClient authClient(@Value("${external.auth.url}") String authUrl,
                                WebClient.Builder builder) {
        return builder.baseUrl(authUrl).build();
    }

    @Bean("apiClient")
    @Primary
    public WebClient apiClient(@Value("${external.base-url}") String baseUrl,
                               WebClient.Builder builder) {
        return builder.baseUrl(baseUrl).build();
    }
}

