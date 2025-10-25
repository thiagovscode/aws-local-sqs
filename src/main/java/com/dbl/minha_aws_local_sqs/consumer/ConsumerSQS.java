package com.dbl.minha_aws_local_sqs.consumer;

import com.dbl.minha_aws_local_sqs.dto.SaidaProcessamento;
import com.dbl.minha_aws_local_sqs.dto.SolicitacaoVeicular;
import com.dbl.minha_aws_local_sqs.producer.SqsMessagePublisher;
import com.dbl.minha_aws_local_sqs.service.ProcessingService; // <- este deve retornar SaidaProcessamento
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ConsumerSQS {

    private final ProcessingService processing;   // process(SolicitacaoVeicular) -> SaidaProcessamento
    private final SqsMessagePublisher publisher;
    private final ObjectMapper objectMapper;

    @SqsListener("${app.sqs.input-queue:pizza-order}")
    public void listen(String body) {
        System.out.println("RAW message: " + body);

        SolicitacaoVeicular msg = tryParse(body);
        if (msg == null) {
            System.err.println("❌ Payload inválido. Ignorando: " + body);
            return;
        }

        SaidaProcessamento saida = processing.process(msg);
        publisher.publish(saida);
    }

    /** Tenta JSON primeiro; se falhar, faz fallback para texto ("chave: valor" ou 3 linhas). */
    private SolicitacaoVeicular tryParse(String body) {
        // 1) JSON direto
        try {
            return objectMapper.readValue(body, SolicitacaoVeicular.class);
        } catch (Exception ignored) { /* tenta fallback */ }

        // 2) "chave: valor"
        Map<String, String> map = new LinkedHashMap<>();
        body.lines().forEach(l -> {
            int i = l.indexOf(':');
            if (i > 0) {
                String k = l.substring(0, i).trim().toLowerCase();
                String v = l.substring(i + 1).trim();
                map.put(k, v);
            }
        });

        if (!map.isEmpty()) {
            String id  = first(map, "id", "id transacao", "id_transacao");
            String poc = first(map, "placa ou chassi", "placa_ou_chassi", "placa", "chassi");
            String prod= first(map, "produto", "payload");
            if (notBlank(id) && notBlank(poc) && notBlank(prod)) {
                return new SolicitacaoVeicular(id.trim(), poc.trim(), prod.trim());
            }
        }

        // 3) 3 linhas simples (id / placaOuChassi / produto)
        String[] lines = body.split("\\R");
        if (lines.length >= 3) {
            return new SolicitacaoVeicular(
                    lines[0].trim(),
                    lines[1].trim(),
                    lines[2].trim()
            );
        }

        return null;
    }

    private static String first(Map<String,String> m, String... keys) {
        for (String k : keys) {
            String v = m.get(k);
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
