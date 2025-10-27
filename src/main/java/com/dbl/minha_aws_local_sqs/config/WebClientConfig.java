package com.dbl.minha_aws_local_sqs.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    private HttpClient httpClient(int connectMs, int readMs, int writeMs) {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectMs)
                .responseTimeout(Duration.ofMillis(readMs))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(readMs, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(writeMs, TimeUnit.MILLISECONDS)));
    }

    @Bean("apiClient")
    @Primary
    public WebClient apiClient(
            @Value("${external.base-url:http://localhost:8080}") String baseUrl,
            @Value("${external.timeout-ms:5000}") int timeoutMs,
            WebClient.Builder builder
    ) {
        return builder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(
                        httpClient(timeoutMs, timeoutMs, timeoutMs)))
                .build();
    }
}
