package com.dbl.minha_aws_local_sqs.service;

import com.dbl.minha_aws_local_sqs.dto.SolicitacaoVeicular;
import com.dbl.minha_aws_local_sqs.dto.SaidaProcessamento;
import com.dbl.minha_aws_local_sqs.service.ExternalApiClient.ProcessRequest;
import com.dbl.minha_aws_local_sqs.service.ExternalApiClient.ProcessResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service
public class ProcessingService {
    private final ExternalApiClient external;
    private final String clientId, clientSecret, scope;

    public ProcessingService(
            ExternalApiClient external,
            @Value("${external.auth.client-id:local-id}") String clientId,
            @Value("${external.auth.client-secret:local-secret}") String clientSecret,
            @Value("${external.auth.scope:default}") String scope
    ) {
        this.external = external;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
    }

    public SaidaProcessamento process(SolicitacaoVeicular msg) {
        String token = external.fetchToken(clientId, clientSecret, scope);
        ProcessRequest req = new ProcessRequest(
                msg.id(),
                msg.produto(),
                "from-sqs",
                msg.placaOuChassi()
        );
        ProcessResponse resp = external.callProcess(token, req);
        return SaidaProcessamento.of(msg, resp);
    }
}
