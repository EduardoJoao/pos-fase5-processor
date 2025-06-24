package com.core.domain.video.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EmailNotificationServiceTest {

    private JavaMailSender mailSender;
    private EmailNotificationService service;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        service = new EmailNotificationService(mailSender);
        // Set emailFrom via reflection
        try {
            var field = EmailNotificationService.class.getDeclaredField("emailFrom");
            field.setAccessible(true);
            field.set(service, "noreply@videoservico.com");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void sendProcessingErrorEmail_shouldSendEmail() {
        service.sendProcessingErrorEmail("destinatario@teste.com", "vid123", "video.mp4", "erro");
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getTo()).contains("destinatario@teste.com");
        assertThat(msg.getFrom()).isEqualTo("noreply@videoservico.com");
        assertThat(msg.getSubject()).contains("Falha no processamento");
        assertThat(msg.getText()).contains("video.mp4");
    }
}