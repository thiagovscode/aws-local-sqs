package com.dbl.minha_aws_local_sqs.consumer;

import com.dbl.minha_aws_local_sqs.dto.SaidaProcessamento;
import com.dbl.minha_aws_local_sqs.dto.SolicitacaoVeicular;
import com.dbl.minha_aws_local_sqs.error.ExternalServiceException;
import com.dbl.minha_aws_local_sqs.error.ParsingException;
import com.dbl.minha_aws_local_sqs.error.ValidationException;
import com.dbl.minha_aws_local_sqs.producer.SqsMessagePublisher;
import com.dbl.minha_aws_local_sqs.service.ProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConsumerSQS {

    private final ProcessingService processing;
    private final SqsMessagePublisher publisher;
    private final ObjectMapper objectMapper;

    @SqsListener("${app.sqs.input-queue:pizza-order}")
    public void listen(String body, @Headers Map<String, Object> headers) {

        final String messageId    = (String) headers.get("MessageId");
        final String receiveCount = (String) headers.get("ApproximateReceiveCount");
        final String sentTs       = (String) headers.get("SentTimestamp");
        final long t0 = System.currentTimeMillis();

        log.info("SQS received id={} attempts={} sentTs={} bodyLen={}",
                messageId, receiveCount, sentTs, body == null ? 0 : body.length());

        try {
            SolicitacaoVeicular msg = tryParse(body);
            validate(msg);

            SaidaProcessamento saida = processing.process(msg);
            publisher.publish(saida);

            log.info("SQS processed id={} corrId={} status={} in {}ms",
                    messageId, saida.correlationId(), saida.status(), System.currentTimeMillis() - t0);

        } catch (ParsingException | ValidationException e) {
            log.warn("Discarding id={} due to client error: {}", messageId, e.getMessage());
        } catch (ExternalServiceException e) {
            log.error("External failure id={}: {}", messageId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error id={}", messageId, e);
            throw e;
        }
    }

    // ---- helpers ----

    private void validate(SolicitacaoVeicular msg) {
        if (msg == null) throw new ValidationException("Mensagem nula");
        if (isBlank(msg.id())) throw new ValidationException("Campo 'id' obrigatório");
        if (isBlank(msg.placaOuChassi())) throw new ValidationException("Campo 'placaOuChassi' obrigatório");
        if (isBlank(msg.produto())) throw new ValidationException("Campo 'produto' obrigatório");
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    private SolicitacaoVeicular tryParse(String body) {
        if (body == null || body.isBlank()) throw new ParsingException("Body vazio");

        // 1) JSON
        try { return objectMapper.readValue(body, SolicitacaoVeicular.class); }
        catch (Exception ignore) { /* fallback */ }

        // 2) "chave: valor"
        Map<String, String> map = new LinkedHashMap<>();
        body.lines().forEach(l -> {
            int i = l.indexOf(':');
            if (i > 0) map.put(l.substring(0, i).trim().toLowerCase(), l.substring(i + 1).trim());
        });
        if (!map.isEmpty()) {
            String id  = first(map, "id", "id transacao", "id_transacao");
            String poc = first(map, "placa ou chassi", "placa_ou_chassi", "placa", "chassi");
            String prod= first(map, "produto", "payload");
            if (!isBlank(id) && !isBlank(poc) && !isBlank(prod))
                return new SolicitacaoVeicular(id, poc, prod);
        }

        // 3) 3 linhas
        String[] lines = body.split("\\R");
        if (lines.length >= 3)
            return new SolicitacaoVeicular(lines[0].trim(), lines[1].trim(), lines[2].trim());

        throw new ParsingException("Formato de payload não reconhecido");
    }

    private static String first(Map<String, String> map, String... keys) {
        for (String k : keys) {
            String v = map.get(k);
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
