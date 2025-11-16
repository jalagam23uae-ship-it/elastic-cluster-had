package com.dynamic.xsd.service;

import com.dynamic.xsd.config.DynamicServiceProperties;
import com.dynamic.xsd.domain.entity.User;
import com.dynamic.xsd.dto.AuthenticationRequest;
import com.dynamic.xsd.dto.AuthenticationResponse;
import com.dynamic.xsd.repository.UserRepository;
import com.dynamic.xsd.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for handling authentication operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DynamicServiceProperties properties;

    /**
     * Authenticates user and returns JWT tokens.
     */
    public AuthenticationResponse login(AuthenticationRequest request) {
        log.info("Attempting authentication for user: {}", request.getUsername());

        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(request.getUsername());

        // Update last login time
        userRepository.findByUsername(request.getUsername()).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        });

        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("User {} authenticated successfully", request.getUsername());

        return AuthenticationResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(properties.getSecurity().getJwt().getExpiration())
            .username(user.getUsername())
            .role(user.getRole().name())
            .build();
    }

    /**
     * Refreshes access token using refresh token.
     */
    public AuthenticationResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String username = tokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Create authentication object
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user.getUsername(),
            null,
            null
        );

        String newAccessToken = tokenProvider.generateToken(authentication);
        String newRefreshToken = tokenProvider.generateRefreshToken(username);

        return AuthenticationResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .tokenType("Bearer")
            .expiresIn(properties.getSecurity().getJwt().getExpiration())
            .username(user.getUsername())
            .role(user.getRole().name())
            .build();
    }
}
