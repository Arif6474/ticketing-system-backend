package com.ticket.system.service;

import com.ticket.system.dto.request.*;
import com.ticket.system.dto.response.AuthResponse;
import com.ticket.system.dto.response.GenericResponse;
import com.ticket.system.dto.response.UserResponse;
import com.ticket.system.entity.PasswordResetToken;
import com.ticket.system.entity.User;
import com.ticket.system.exception.AppException;
import com.ticket.system.repository.PasswordResetTokenRepository;
import com.ticket.system.repository.UserRepository;
import com.ticket.system.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final long RESET_TOKEN_EXPIRATION_HOURS = 1;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordResetTokenRepository resetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!user.isActive()) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "User account is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String accessToken = tokenProvider.generateToken(user);
        return new AuthResponse(accessToken, UserResponse.fromEntity(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!user.isActive()) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "User account is inactive");
        }

        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        if (!user.isActive()) {
            throw new AppException(HttpStatus.FORBIDDEN, "User account is inactive");
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setMobile(request.getMobile());
        user.setDesignation(request.getDesignation());
        user.setOffice(request.getOffice());

        User updatedUser = userRepository.save(user);
        return UserResponse.fromEntity(updatedUser);
    }

    @Transactional
    public GenericResponse changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        if (request.getNewPassword().length() < 8) {
            throw new AppException(HttpStatus.BAD_REQUEST, "New password must be at least 8 characters long");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return new GenericResponse("Password changed successfully");
    }

    @Transactional
    public GenericResponse forgotPassword(ForgotPasswordRequest request) {
        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(request.getEmail());

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.isActive()) {
                resetTokenRepository.deleteByUser(user);

                String rawToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
                String tokenHash = hashToken(rawToken);
                OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(RESET_TOKEN_EXPIRATION_HOURS);

                PasswordResetToken tokenEntity = new PasswordResetToken(user, tokenHash, expiresAt);
                resetTokenRepository.save(tokenEntity);

                // Development Email Stub (Logging only, never returning in response)
                logger.info("========== DEVELOPMENT EMAIL STUB ==========");
                logger.info("Password reset requested for email: {}", user.getEmail());
                logger.info("Raw Reset Token (Dev Only): {}", rawToken);
                logger.info("Expires At: {}", expiresAt);
                logger.info("=============================================");
            }
        }

        return new GenericResponse("If an account exists for this email, password reset instructions have been sent.");
    }

    @Transactional
    public GenericResponse resetPassword(ResetPasswordRequest request) {
        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw new AppException(HttpStatus.BAD_REQUEST, "New password must be at least 8 characters long");
        }

        String tokenHash = hashToken(request.getToken());
        PasswordResetToken resetToken = resetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Invalid or expired password reset token"));

        if (resetToken.isUsed() || resetToken.isExpired()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Invalid or expired password reset token");
        }

        User user = resetToken.getUser();
        if (!user.isActive()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Invalid or expired password reset token");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsedAt(OffsetDateTime.now());
        resetTokenRepository.save(resetToken);

        return new GenericResponse("Password reset successfully");
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
