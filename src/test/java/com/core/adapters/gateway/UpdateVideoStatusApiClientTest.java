package com.core.adapters.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UpdateVideoStatusApiClientTest {

    @Mock
    private RestTemplate restTemplate;

    private UpdateVideoStatusApiClient apiClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        apiClient = new UpdateVideoStatusApiClient(restTemplate);
        // Set coreServiceUrl via reflection (since @Value is not processed in unit tests)
        try {
            var field = UpdateVideoStatusApiClient.class.getDeclaredField("coreServiceUrl");
            field.setAccessible(true);
            field.set(apiClient, "http://localhost:8081");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void execute_shouldCallRestTemplate() {
        apiClient.execute("client", "video", "SUCCESS", null, "zip.zip", "1 MB");
        verify(restTemplate).exchange(
                eq("http://localhost:8081/videos/video/status"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Void.class)
        );
    }
}