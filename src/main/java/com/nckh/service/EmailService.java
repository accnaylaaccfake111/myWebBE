package com.nckh.service;

public interface EmailService {
    
    void sendVerificationEmail(String to, String username, String verificationToken);
    
    void sendPasswordResetEmail(String to, String username, String resetToken);
    
    void sendWelcomeEmail(String to, String username);
}