package com.nckh.service.impl;

import com.nckh.dto.request.LoginRequest;
import com.nckh.dto.request.RegisterRequest;
import com.nckh.dto.response.AuthResponse;
import com.nckh.entity.User;
import com.nckh.entity.User.Role;
import com.nckh.entity.User.UserStatus;
import com.nckh.exception.BadRequestException;
import com.nckh.exception.ResourceNotFoundException;
import com.nckh.repository.UserRepository;
import com.nckh.security.JwtTokenProvider;
import com.nckh.service.AuthService;
import com.nckh.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;
    
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(),
                            request.getPassword()
                    )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            User user = (User) authentication.getPrincipal();
            
            // Check if account is locked
            if (!user.isAccountNonLocked()) {
                throw new BadRequestException("Account is locked. Please try again later.");
            }
            
            // Generate tokens
            String accessToken = tokenProvider.generateAccessToken(user);
            String refreshToken = tokenProvider.generateRefreshToken(user);
            
            // Update user login info
            user.setLastLoginAt(LocalDateTime.now());
            user.setLastLoginIp(ipAddress);
            user.setFailedLoginAttempts(0);
            user.setRefreshToken(refreshToken);
            user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
            userRepository.save(user);
            
            log.info("User logged in successfully: {}", user.getUsername());
            
            return buildAuthResponse(accessToken, refreshToken, user);
            
        } catch (BadCredentialsException e) {
            handleFailedLogin(request.getUsernameOrEmail());
            throw new BadRequestException("Invalid username/email or password");
        }
    }
    
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken!");
        }
        
        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already in use!");
        }
        
        // Create new user
        Set<Role> userRoles = new HashSet<>();
        userRoles.add(Role.USER);
        
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .status(UserStatus.PENDING_VERIFICATION)
                .roles(userRoles)
                .emailVerified(false)
                .build();
        
        // Generate verification token
        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        
        User savedUser = userRepository.save(user);
        
        // Generate tokens
        String accessToken = tokenProvider.generateAccessToken(savedUser);
        String refreshToken = tokenProvider.generateRefreshToken(savedUser);
        
        savedUser.setRefreshToken(refreshToken);
        savedUser.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
        userRepository.save(savedUser);
        
        log.info("New user registered: {}", savedUser.getUsername());
        
        // Send verification email
        emailService.sendVerificationEmail(
            savedUser.getEmail(), 
            savedUser.getUsername(), 
            verificationToken
        );
        
        return buildAuthResponse(accessToken, refreshToken, savedUser);
    }
    
    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid refresh token");
        }
        
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with this refresh token"));
        
        if (user.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Refresh token has expired");
        }
        
        String newAccessToken = tokenProvider.generateAccessToken(user);
        String newRefreshToken = tokenProvider.generateRefreshToken(user);
        
        user.setRefreshToken(newRefreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
        userRepository.save(user);
        
        return buildAuthResponse(newAccessToken, newRefreshToken, user);
    }
    
    @Override
    @Transactional
    public void logout(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        user.setRefreshToken(null);
        user.setRefreshTokenExpiry(null);
        userRepository.save(user);
        
        SecurityContextHolder.clearContext();
        log.info("User logged out: {}", username);
    }
    
    private void handleFailedLogin(String usernameOrEmail) {
        userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .ifPresent(user -> {
                    int attempts = user.getFailedLoginAttempts() + 1;
                    user.setFailedLoginAttempts(attempts);
                    
                    if (attempts >= 5) {
                        user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
                        log.warn("User account locked due to too many failed attempts: {}", user.getUsername());
                    }
                    
                    userRepository.save(user);
                });
    }
    
    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, User user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationTime())
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .avatarUrl(user.getAvatarUrl())
                        .roles(user.getRoles().stream()
                                .map(Enum::name)
                                .collect(Collectors.toList()))
                        .emailVerified(user.isEmailVerified())
                        .createdAt(user.getCreatedAt())
                        .build())
                .build();
    }
    
    @Override
    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid verification token"));
        
        if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Verification token has expired");
        }
        
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        
        userRepository.save(user);
        
        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
        
        log.info("Email verified successfully for user: {}", user.getUsername());
    }
    
    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with this email"));
        
        if (user.isEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }
        
        // Generate new verification token
        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        
        userRepository.save(user);
        
        // Send verification email
        emailService.sendVerificationEmail(
            user.getEmail(), 
            user.getUsername(), 
            verificationToken
        );
        
        log.info("Verification email resent to: {}", email);
    }
}