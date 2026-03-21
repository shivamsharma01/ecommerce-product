package com.mcart.product;

import com.google.cloud.spring.data.firestore.FirestoreTemplate;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
		properties = {
				"spring.cloud.gcp.firestore.enabled=false",
				"spring.cloud.gcp.pubsub.enabled=false",
				"app.security.enabled=false"
		}
)
class ProductApplicationTests {
	@MockBean
	private FirestoreTemplate firestoreTemplate;

	@MockBean
	private PubSubTemplate pubSubTemplate;

	@Test
	void contextLoads() {
	}
}
