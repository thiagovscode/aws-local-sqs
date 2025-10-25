package com.dbl.minha_aws_local_sqs.infrastructure.out.sqs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.net.URI;

@Configuration
@Profile("local")
public class SqsClientConfig {

    @Bean
    @Profile("local")
    public SqsAsyncClient localSqsAsyncClient(
            @Value("${aws.region}") String region
    ) {
        return SqsAsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .endpointOverride(URI.create("http://localhost:4566"))
                .build();
    }

    @Bean
    @Profile("!local")
    public SqsAsyncClient defaultSqsAsyncClient(
            @Value("${aws.region}") String region
    ) {
        // Em cloud, usa a chain padrão (IAM role, env, etc.)
        return SqsAsyncClient.builder()
                .region(Region.of(region))
                .build();
    }
}
