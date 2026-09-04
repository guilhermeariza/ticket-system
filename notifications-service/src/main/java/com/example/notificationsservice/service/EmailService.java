package com.example.notificationsservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Send a simple text email
     */
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Send an HTML email
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = isHtml

            mailSender.send(message);
            log.info("HTML email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to: {}", to, e);
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    /**
     * Send order confirmation email
     */
    public void sendOrderConfirmation(String to, Long orderId, String totalAmount) {
        String subject = "Order Confirmation - Order #" + orderId;
        String htmlBody = buildOrderConfirmationEmail(orderId, totalAmount);
        sendHtmlEmail(to, subject, htmlBody);
    }

    /**
     * Send payment confirmation email
     */
    public void sendPaymentConfirmation(String to, Long orderId, boolean success) {
        String subject = success ?
            "Payment Successful - Order #" + orderId :
            "Payment Failed - Order #" + orderId;
        String htmlBody = buildPaymentConfirmationEmail(orderId, success);
        sendHtmlEmail(to, subject, htmlBody);
    }

    private String buildOrderConfirmationEmail(Long orderId, String totalAmount) {
        return "<!DOCTYPE html>" +
               "<html>" +
               "<head><style>" +
               "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
               ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
               ".header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }" +
               ".content { padding: 20px; background-color: #f9f9f9; }" +
               ".footer { text-align: center; padding: 20px; font-size: 12px; color: #666; }" +
               "</style></head>" +
               "<body>" +
               "<div class='container'>" +
               "<div class='header'><h1>Order Confirmation</h1></div>" +
               "<div class='content'>" +
               "<p>Thank you for your order!</p>" +
               "<p><strong>Order ID:</strong> " + orderId + "</p>" +
               "<p><strong>Total Amount:</strong> $" + totalAmount + "</p>" +
               "<p>Your order has been received and is being processed. You will receive another email once payment is confirmed.</p>" +
               "</div>" +
               "<div class='footer'>" +
               "<p>Thank you for choosing our ticket system!</p>" +
               "</div>" +
               "</div>" +
               "</body></html>";
    }

    private String buildPaymentConfirmationEmail(Long orderId, boolean success) {
        String color = success ? "#4CAF50" : "#f44336";
        String status = success ? "Payment Successful" : "Payment Failed";
        String message = success ?
            "Your payment has been processed successfully. Your tickets are now confirmed!" :
            "Unfortunately, your payment could not be processed. Please try again or contact support.";

        return "<!DOCTYPE html>" +
               "<html>" +
               "<head><style>" +
               "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
               ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
               ".header { background-color: " + color + "; color: white; padding: 20px; text-align: center; }" +
               ".content { padding: 20px; background-color: #f9f9f9; }" +
               ".footer { text-align: center; padding: 20px; font-size: 12px; color: #666; }" +
               "</style></head>" +
               "<body>" +
               "<div class='container'>" +
               "<div class='header'><h1>" + status + "</h1></div>" +
               "<div class='content'>" +
               "<p><strong>Order ID:</strong> " + orderId + "</p>" +
               "<p>" + message + "</p>" +
               "</div>" +
               "<div class='footer'>" +
               "<p>Thank you for choosing our ticket system!</p>" +
               "</div>" +
               "</div>" +
               "</body></html>";
    }
}
