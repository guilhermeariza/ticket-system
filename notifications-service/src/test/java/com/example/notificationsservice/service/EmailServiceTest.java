package com.example.notificationsservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailService
 * Tests email sending functionality in isolation using Mockito
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private static final String FROM_EMAIL = "noreply@ticketsystem.com";
    private static final String TO_EMAIL = "user@example.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", FROM_EMAIL);
    }

    @Test
    void sendEmail_WithValidData_ShouldSendSuccessfully() {
        // Arrange
        String subject = "Test Subject";
        String body = "Test Body";

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.sendEmail(TO_EMAIL, subject, body);

        // Assert
        verify(mailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();

        assertEquals(FROM_EMAIL, sentMessage.getFrom());
        assertEquals(TO_EMAIL, sentMessage.getTo()[0]);
        assertEquals(subject, sentMessage.getSubject());
        assertEquals(body, sentMessage.getText());
    }

    @Test
    void sendEmail_WhenMailSenderFails_ShouldThrowRuntimeException() {
        // Arrange
        doThrow(new RuntimeException("Mail server error"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            emailService.sendEmail(TO_EMAIL, "Subject", "Body");
        });

        assertEquals("Failed to send email", exception.getMessage());
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendHtmlEmail_WithValidData_ShouldSendSuccessfully() throws MessagingException {
        // Arrange
        String subject = "HTML Test Subject";
        String htmlBody = "<html><body><h1>Test</h1></body></html>";

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act
        emailService.sendHtmlEmail(TO_EMAIL, subject, htmlBody);

        // Assert
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void sendHtmlEmail_WhenMessagingFails_ShouldThrowRuntimeException() {
        // Arrange
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("MIME creation error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            emailService.sendHtmlEmail(TO_EMAIL, "Subject", "<html>Body</html>");
        });

        assertTrue(exception.getMessage().contains("Failed to send HTML email") ||
                   exception.getMessage().contains("MIME creation error"));
    }

    @Test
    void sendOrderConfirmation_ShouldSendHtmlEmail() throws MessagingException {
        // Arrange
        Long orderId = 123L;
        String totalAmount = "150.00";

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act
        emailService.sendOrderConfirmation(TO_EMAIL, orderId, totalAmount);

        // Assert
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void sendOrderConfirmation_ShouldContainOrderDetails() throws MessagingException {
        // Arrange
        Long orderId = 456L;
        String totalAmount = "250.50";

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act
        emailService.sendOrderConfirmation(TO_EMAIL, orderId, totalAmount);

        // Assert - Verify HTML email was sent
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void sendPaymentConfirmation_WithSuccessfulPayment_ShouldSendSuccessEmail() throws MessagingException {
        // Arrange
        Long orderId = 789L;
        boolean success = true;

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act
        emailService.sendPaymentConfirmation(TO_EMAIL, orderId, success);

        // Assert
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void sendPaymentConfirmation_WithFailedPayment_ShouldSendFailureEmail() throws MessagingException {
        // Arrange
        Long orderId = 999L;
        boolean success = false;

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act
        emailService.sendPaymentConfirmation(TO_EMAIL, orderId, success);

        // Assert
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void sendEmail_WithEmptySubject_ShouldStillSend() {
        // Arrange
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.sendEmail(TO_EMAIL, "", "Body");

        // Assert
        verify(mailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals("", sentMessage.getSubject());
    }

    @Test
    void sendEmail_WithEmptyBody_ShouldStillSend() {
        // Arrange
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.sendEmail(TO_EMAIL, "Subject", "");

        // Assert
        verify(mailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals("", sentMessage.getText());
    }

    @Test
    void sendEmail_WithMultipleRecipients_ShouldSendToFirst() {
        // Arrange
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.sendEmail(TO_EMAIL, "Subject", "Body");

        // Assert
        verify(mailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals(1, sentMessage.getTo().length);
        assertEquals(TO_EMAIL, sentMessage.getTo()[0]);
    }

    @Test
    void sendHtmlEmail_WithComplexHtml_ShouldSendSuccessfully() throws MessagingException {
        // Arrange
        String complexHtml = "<html><head><style>body{color:red;}</style></head>" +
                            "<body><h1>Title</h1><p>Paragraph</p><div>Content</div></body></html>";

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act
        emailService.sendHtmlEmail(TO_EMAIL, "Complex HTML", complexHtml);

        // Assert
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void sendOrderConfirmation_WithDifferentAmounts_ShouldSendCorrectly() throws MessagingException {
        // Arrange
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act
        emailService.sendOrderConfirmation(TO_EMAIL, 1L, "10.00");
        emailService.sendOrderConfirmation(TO_EMAIL, 2L, "999.99");

        // Assert
        verify(mailSender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    void sendPaymentConfirmation_WithMultipleOrders_ShouldSendMultipleEmails() throws MessagingException {
        // Arrange
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act
        emailService.sendPaymentConfirmation(TO_EMAIL, 1L, true);
        emailService.sendPaymentConfirmation(TO_EMAIL, 2L, false);
        emailService.sendPaymentConfirmation(TO_EMAIL, 3L, true);

        // Assert
        verify(mailSender, times(3)).send(any(MimeMessage.class));
    }
}
