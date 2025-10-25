package com.dbl.minha_aws_local_sqs;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;


@ActiveProfiles("test")
@SpringBootTest(properties = "spring.cloud.aws.sqs.enabled=false")
class MinhaAwsLocalSqsApplicationTests {

	@Test
	void contextLoads() {
	}

}
