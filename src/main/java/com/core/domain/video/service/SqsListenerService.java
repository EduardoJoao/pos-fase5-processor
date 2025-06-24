package com.core.domain.video.service;

import com.core.domain.video.dto.VideoProcessRequest;
import com.core.domain.video.usecase.VideoProcessingWorkflowUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class SqsListenerService {
    private final SqsClient sqsClient;
    private final VideoProcessingWorkflowUseCase videoProcessingWorkflowUseCase;
    private final ObjectMapper objectMapper;
    
    @Value("${sqs.process}")
    private String processingQueueUrl;
    
    @Value("${sqs.threads}")
    private int processingThreads;

    private ExecutorService executorService;

    @PostConstruct
    public void init() {
        executorService = Executors.newFixedThreadPool(processingThreads);
        executorService.submit(this::listenAndProcessContinuously);
    }

    private void listenAndProcessContinuously() {
        while (true) {
            try {
                List<Message> messages = receiveMessages(processingQueueUrl);
                for (Message message : messages) {
                    executorService.submit(() -> {
                        try {
                            VideoProcessRequest request = convertMessageToRequest(message);
                            videoProcessingWorkflowUseCase.processVideo(request);
                        } catch (Exception e) {
                            System.err.println("Erro ao processar mensagem: " + e.getMessage());
                        } finally {
                            deleteMessage(processingQueueUrl, message.receiptHandle());
                        }
                    });
                }
                Thread.sleep(150);
            } catch (Exception e) {
                System.err.println("Erro ao escutar fila: " + e.getMessage());
                try { Thread.sleep(5000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }
    
    private VideoProcessRequest convertMessageToRequest(Message message) throws Exception {
        return objectMapper.readValue(message.body(), VideoProcessRequest.class);
    }

    public List<Message> receiveMessages(String queueUrl) {
        ReceiveMessageResponse response = sqsClient.receiveMessage(
                ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(10)
                        .waitTimeSeconds(20)
                        .build()
        );
        return response.messages();
    }

    public void deleteMessage(String queueUrl, String receiptHandle) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .build());
    }


}