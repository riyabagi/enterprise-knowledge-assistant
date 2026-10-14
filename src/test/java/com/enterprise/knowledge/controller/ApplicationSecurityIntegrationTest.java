package com.enterprise.knowledge.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApplicationSecurityIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void servesStaticFrontend() {
        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);

        assertEquals(200, response.getStatusCode().value());
        assertThat(response.getBody(), containsString("<title>Enterprise Knowledge Assistant</title>"));
    }

    @Test
    void protectsKnowledgeApiWhenUnauthenticated() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/knowledge/documents", String.class);

        assertEquals(401, response.getStatusCode().value());
        MediaType contentType = response.getHeaders().getContentType();
        assertNotNull(contentType);
        assertTrue(contentType.isCompatibleWith(MediaType.APPLICATION_JSON));
        assertNull(response.getHeaders().getFirst("WWW-Authenticate"));
    }
}
