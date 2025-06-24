package com.core.domain.video.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsListenerServiceTest {

    @Mock
    private SqsClient sqsClient;

    @InjectMocks
    private SqsListenerService service;

    @Test
    void receiveMessages_shouldReturnEmptyListWhenNoMessages() {
        // Arrange: configure o mock para retornar uma resposta vazia
        ReceiveMessageResponse emptyResponse = ReceiveMessageResponse.builder()
                .messages(Collections.emptyList())
                .build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(emptyResponse);

        // Act
        List<Message> messages = service.receiveMessages("fake-queue-url");

        // Assert
        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }

    @Test
    void receiveMessages_shouldReturnMessages() {
        Message msg = Message.builder().body("test").build();
        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                .messages(msg)
                .build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(response);

        List<Message> messages = service.receiveMessages("fake-queue-url");

        assertNotNull(messages);
        assertEquals(1, messages.size());
        assertEquals("test", messages.get(0).body());
    }

    @Test
    void deleteMessage_shouldCallSqsClient() {
        DeleteMessageResponse response = DeleteMessageResponse.builder().build();
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class))).thenReturn(response);

        service.deleteMessage("queue-url", "receipt-handle");

        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }
}