package com.nckh.service;

import com.nckh.dto.request.LoginRequest;
import com.nckh.dto.request.RegisterRequest;
import com.nckh.dto.response.AuthResponse;

public interface AuthService {
    
    AuthResponse login(LoginRequest request, String ipAddress);
    
    AuthResponse register(RegisterRequest request);
    
    AuthResponse refreshToken(String refreshToken);
    
    void logout(String username);
    
    void verifyEmail(String token);
    
    void resendVerificationEmail(String email);
}