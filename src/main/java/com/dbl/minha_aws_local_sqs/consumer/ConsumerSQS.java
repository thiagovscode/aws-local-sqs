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

    private final ProcessingService processing;     // regra de negócio
    private final SqsMessagePublisher publisher;    // publica na fila de saída
    private final ObjectMapper objectMapper;        // parse JSON

    @SqsListener("${app.sqs.input-queue:pizza-order}")
    public void listen(String body, @Headers Map<String, Object> headers) {
        final String messageId = (String) headers.get("MessageId");
        final String receiveCount = (String) headers.get("ApproximateReceiveCount");
        final String sentAt = (String) headers.get("SentTimestamp");
        final long t0 = System.currentTimeMillis();

        log.info("msgId={} attempts={} sentAt={} took={}ms",
                messageId,
                receiveCount,
                sentAt,
                System.currentTimeMillis() - t0
        );

        try {
            // 1) parse resiliente
            SolicitacaoVeicular msg = tryParse(body);

            // 2) validação de campos obrigatórios
            validate(msg);

            // 3) processa
            SaidaProcessamento saida = processing.process(msg);

            // 4) publica
            publisher.publish(saida);

            log.info("SQS processed messageId={} corrId={} in {}ms",
                    messageId, msg.id(), System.currentTimeMillis() - t0);

        } catch (ParsingException | ValidationException e) {
            // Erro “do cliente” (payload ruim). Normalmente NÃO vale reprocessar.
            log.warn("Discarding messageId={} due to client error: {}", messageId, e.getMessage());
            // Se quiser, envie para uma fila de erro da aplicação:
            // errorPublisher.publish(new ErrorEnvelope(messageId, body, e.getMessage(), Instant.now()));
            // Não relançar => evita retry infinito.
        } catch (ExternalServiceException e) {
            // Dependência externa falhou (HTTP/timeout) → relança para retry/redrive DLQ nativa.
            log.error("External dependency failure for messageId={}: {}", messageId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            // Falha inesperada → relança (também vai para retry/DLQ).
            log.error("Unexpected error for messageId={}", messageId, e);
            throw e;
        }
    }

    /**
     * Validação simples de required fields.
     */
    private void validate(SolicitacaoVeicular msg) {
        if (msg == null) throw new ValidationException("Mensagem nula");
        if (isBlank(msg.id())) throw new ValidationException("Campo 'id' obrigatório");
        if (isBlank(msg.placaOuChassi())) throw new ValidationException("Campo 'placaOuChassi' obrigatório");
        if (isBlank(msg.produto())) throw new ValidationException("Campo 'produto' obrigatório");
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Tenta JSON; fallback para "chave: valor"; depois 3 linhas (id/placaOuChassi/produto).
     */
    private SolicitacaoVeicular tryParse(String body) {
        if (body == null || body.isBlank()) {
            throw new ParsingException("Body vazio");
        }

        // 1) JSON
        try {
            return objectMapper.readValue(body, SolicitacaoVeicular.class);
        } catch (Exception ignored) {
            // segue para fallback
        }

        // 2) "chave: valor"
        Map<String, String> map = new LinkedHashMap<>();
        body.lines().forEach(l -> {
            int i = l.indexOf(':');
            if (i > 0) map.put(l.substring(0, i).trim().toLowerCase(), l.substring(i + 1).trim());
        });
        if (!map.isEmpty()) {
            String id = first(map, "id", "id transacao", "id_transacao");
            String poc = first(map, "placa ou chassi", "placa_ou_chassi", "placa", "chassi");
            String prod = first(map, "produto", "payload");
            if (!isBlank(id) && !isBlank(poc) && !isBlank(prod)) {
                return new SolicitacaoVeicular(id, poc, prod);
            }
        }

        // 3) 3 linhas simples (id / placaOuChassi / produto)
        String[] lines = body.split("\\R");
        if (lines.length >= 3) {
            return new SolicitacaoVeicular(lines[0].trim(), lines[1].trim(), lines[2].trim());
        }

        throw new ParsingException("Formato de payload não reconhecido");
    }

    /**
     * Retorna o primeiro valor não vazio entre várias chaves alternativas.
     */
    private static String first(Map<String, String> map, String... keys) {
        for (String k : keys) {
            String v = map.get(k);
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
