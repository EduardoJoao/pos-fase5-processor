package com.core.domain.video.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SqsPublisherServiceTest {

    private SqsClient sqsClient;
    private SqsPublisherService service;

    @BeforeEach
    void setUp() {
        sqsClient = mock(SqsClient.class);
        service = new SqsPublisherService(sqsClient);
        // Defina o valor do resultQueueUrl via reflexão se necessário
        try {
            var field = SqsPublisherService.class.getDeclaredField("resultQueueUrl");
            field.setAccessible(true);
            field.set(service, "https://fake-queue-url");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void sendMessage_shouldCallSqsClient() {
        String messageBody = "{\"test\":\"ok\"}";
        service.sendMessage(messageBody);

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());
        SendMessageRequest req = captor.getValue();
        assertEquals("https://fake-queue-url", req.queueUrl());
        assertEquals(messageBody, req.messageBody());
    }
}