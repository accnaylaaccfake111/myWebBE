package com.nckh.service.impl;

import com.nckh.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.backend.url}")
    private String backendUrl;

    @Override
    public void sendVerificationEmail(String to, String username, String verificationToken) {
        try {
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("verificationUrl", backendUrl + "/api/auth/verify-email?token=" + verificationToken);
            
            String htmlContent = templateEngine.process("verification-email", context);
            
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Xác thực tài khoản - NCKH Cultural Arts Platform");
            helper.setText(htmlContent, true);
            
            mailSender.send(mimeMessage);
            log.info("Verification email sent successfully to: {}", to);
            
        } catch (MessagingException e) {
            log.error("Failed to send verification email to: {}", to, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    @Override
    public void sendPasswordResetEmail(String to, String username, String resetToken) {
        try {
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("resetUrl", frontendUrl + "/reset-password?token=" + resetToken);
            
            String htmlContent = templateEngine.process("password-reset-email", context);
            
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Đặt lại mật khẩu - NCKH Cultural Arts Platform");
            helper.setText(htmlContent, true);
            
            mailSender.send(mimeMessage);
            log.info("Password reset email sent successfully to: {}", to);
            
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", to, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    @Override
    public void sendWelcomeEmail(String to, String username) {
        try {
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("platformUrl", frontendUrl);
            
            String htmlContent = templateEngine.process("welcome-email", context);
            
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Chào mừng đến với NCKH Cultural Arts Platform!");
            helper.setText(htmlContent, true);
            
            mailSender.send(mimeMessage);
            log.info("Welcome email sent successfully to: {}", to);
            
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {}", to, e);
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }
}