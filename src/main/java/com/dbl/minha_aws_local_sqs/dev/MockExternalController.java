package com.dbl.minha_aws_local_sqs.dev;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Profile("local")
public class MockExternalController {

    @PostMapping("/oauth/token")
    public Map<String,Object> token() {
        return Map.of(
                "access_token", "local-token",
                "token_type", "bearer",
                "expires_in", 3600
        );
    }

    @PostMapping("/process")
    public Map<String,String> process(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String,Object> body
    ) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return Map.of("status", "UNAUTHORIZED", "details", "invalid token");
        }
        return Map.of(
                "status", "OK",
                "details", "processado localmente: " + body.getOrDefault("id", "sem-id")
        );
    }
}
