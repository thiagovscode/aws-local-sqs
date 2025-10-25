package com.dbl.minha_aws_local_sqs.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
public class SqsMessagePublisher {

    private final SqsAsyncClient sqsClient;
    private final String outputQueueUrl;
    private final ObjectMapper objectMapper;

    public SqsMessagePublisher(
            SqsAsyncClient sqsClient,
            ObjectMapper objectMapper,
            @Value("${app.sqs.output-queue:pizza-order-out}") String outputQueueName
    ) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;

        this.outputQueueUrl = sqsClient.getQueueUrl(r -> r.queueName(outputQueueName))
                .join()
                .queueUrl();
    }

    public void publish(Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            sqsClient.sendMessage(
                    SendMessageRequest.builder()
                            .queueUrl(outputQueueUrl)
                            .messageBody(json)
                            .build()
            );
            System.out.println("✅ Enviado p/ fila de saída: " + json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Falha ao serializar payload", e);
        }
    }
}
