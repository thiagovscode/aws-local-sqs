package com.dbl.minha_aws_local_sqs.producer;

import com.dbl.minha_aws_local_sqs.error.PublishingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Slf4j
@Component
public class SqsMessagePublisher {

    private final SqsAsyncClient sqsClient;
    private final String outputQueueUrl;
    private final ObjectMapper objectMapper;

    public SqsMessagePublisher(SqsAsyncClient sqsClient,
                               ObjectMapper objectMapper,
                               @Value("${app.sqs.output-queue:pizza-order-out}") String outputQueueName) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        try {
            this.outputQueueUrl = sqsClient.getQueueUrl(r -> r.queueName(outputQueueName)).join().queueUrl();
        } catch (Exception e) {
            throw new PublishingException("Falha ao resolver URL da fila de saída: " + outputQueueName, e);
        }
    }

    public void publish(Object payload) {
        final String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new PublishingException("Falha ao serializar payload", e);
        }

        try {
            var future = sqsClient.sendMessage(
                    SendMessageRequest.builder()
                            .queueUrl(outputQueueUrl)
                            .messageBody(json)
                            .build()
            );
            var resp = future.join(); // se der erro na AWS SDK, lança CompletionException
            log.info("Published to {} messageId={}", outputQueueUrl, resp.messageId());
        } catch (Exception e) {
            throw new PublishingException("Falha ao publicar na fila de saída", e);
        }
    }
}
