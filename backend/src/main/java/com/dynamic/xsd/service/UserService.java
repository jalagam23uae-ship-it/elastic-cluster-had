package com.dynamic.xsd.service;

import com.dynamic.xsd.domain.entity.User;
import com.dynamic.xsd.domain.enums.UserRole;
import com.dynamic.xsd.dto.UserCreateRequest;
import com.dynamic.xsd.dto.UserResponse;
import com.dynamic.xsd.dto.UserUpdateRequest;
import com.dynamic.xsd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing user operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a new user.
     */
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        log.info("Creating new user: {}", request.getUsername());

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : UserRole.USER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        log.info("User created successfully: {}", savedUser.getUsername());

        return toUserResponse(savedUser);
    }

    /**
     * Updates an existing user.
     */
    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        log.info("Updating user ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Update fields if provided
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already exists: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", updatedUser.getUsername());

        return toUserResponse(updatedUser);
    }

    /**
     * Gets a user by ID.
     */
    public UserResponse getUserById(Long userId) {
        log.debug("Getting user by ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        return toUserResponse(user);
    }

    /**
     * Gets a user by username.
     */
    public UserResponse getUserByUsername(String username) {
        log.debug("Getting user by username: {}", username);

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        return toUserResponse(user);
    }

    /**
     * Lists all users with pagination.
     */
    public Page<UserResponse> listUsers(Pageable pageable) {
        log.debug("Listing users with pagination: {}", pageable);

        return userRepository.findAll(pageable)
            .map(this::toUserResponse);
    }

    /**
     * Lists users by role.
     */
    public List<UserResponse> listUsersByRole(UserRole role) {
        log.debug("Listing users by role: {}", role);

        return userRepository.findByRole(role).stream()
            .map(this::toUserResponse)
            .collect(Collectors.toList());
    }

    /**
     * Lists active users.
     */
    public List<UserResponse> listActiveUsers() {
        log.debug("Listing active users");

        return userRepository.findByActive(true).stream()
            .map(this::toUserResponse)
            .collect(Collectors.toList());
    }

    /**
     * Deletes a user.
     */
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Deleting user ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        userRepository.delete(user);
        log.info("User deleted successfully: {}", user.getUsername());
    }

    /**
     * Deactivates a user (soft delete).
     */
    @Transactional
    public UserResponse deactivateUser(Long userId) {
        log.info("Deactivating user ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setActive(false);
        User updatedUser = userRepository.save(user);

        log.info("User deactivated successfully: {}", updatedUser.getUsername());
        return toUserResponse(updatedUser);
    }

    /**
     * Activates a user.
     */
    @Transactional
    public UserResponse activateUser(Long userId) {
        log.info("Activating user ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setActive(true);
        User updatedUser = userRepository.save(user);

        log.info("User activated successfully: {}", updatedUser.getUsername());
        return toUserResponse(updatedUser);
    }

    /**
     * Changes user password.
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        log.info("Changing password for user ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", user.getUsername());
    }

    /**
     * Gets user count by role.
     */
    public long countUsersByRole(UserRole role) {
        return userRepository.countByRole(role);
    }

    /**
     * Gets total active user count.
     */
    public long countActiveUsers() {
        return userRepository.countByActive(true);
    }

    /**
     * Gets total user count.
     */
    public long countAllUsers() {
        return userRepository.count();
    }

    /**
     * Converts User entity to UserResponse DTO.
     */
    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .role(user.getRole().name())
            .active(user.isActive())
            .createdAt(user.getCreatedAt())
            .lastLoginAt(user.getLastLoginAt())
            .build();
    }
}
