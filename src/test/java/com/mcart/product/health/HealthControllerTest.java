package com.mcart.product.health;

import com.google.cloud.spring.data.firestore.FirestoreTemplate;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
        properties = {
                "spring.cloud.gcp.firestore.enabled=false",
                "spring.cloud.gcp.pubsub.enabled=false",
                "app.security.enabled=false"
        }
)
@AutoConfigureWebTestClient
class HealthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private FirestoreTemplate firestoreTemplate;

    @MockBean
    private PubSubTemplate pubSubTemplate;

    @Test
    void healthReturnsOk() {
        webTestClient.get().uri("/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }
}
