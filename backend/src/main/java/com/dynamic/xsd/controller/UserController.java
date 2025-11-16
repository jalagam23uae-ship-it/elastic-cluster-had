package com.dynamic.xsd.controller;

import com.dynamic.xsd.domain.enums.UserRole;
import com.dynamic.xsd.dto.ApiResponse;
import com.dynamic.xsd.dto.UserCreateRequest;
import com.dynamic.xsd.dto.UserResponse;
import com.dynamic.xsd.dto.UserUpdateRequest;
import com.dynamic.xsd.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for user management operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for managing users")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create User", description = "Create a new user")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("Create user request: {}", request.getUsername());

        try {
            UserResponse user = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user, "User created successfully"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to create user: " + e.getMessage()));
        }
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get User", description = "Get user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable String userId) {
        log.debug("Get user request for ID: {}", userId);

        try {
            UserResponse user = userService.getUserById(userId);
            return ResponseEntity.ok(ApiResponse.success(user, "User retrieved successfully"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to get user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to get user: " + e.getMessage()));
        }
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Get User by Username", description = "Get user by username")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByUsername(@PathVariable String username) {
        log.debug("Get user request for username: {}", username);

        try {
            UserResponse user = userService.getUserByUsername(username);
            return ResponseEntity.ok(ApiResponse.success(user, "User retrieved successfully"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to get user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to get user: " + e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "List Users", description = "List all users with pagination")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listUsers(Pageable pageable) {
        log.debug("List users request");

        try {
            Page<UserResponse> users = userService.listUsers(pageable);
            return ResponseEntity.ok(ApiResponse.success(users, "Users retrieved successfully"));

        } catch (Exception e) {
            log.error("Failed to list users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to list users: " + e.getMessage()));
        }
    }

    @GetMapping("/role/{role}")
    @Operation(summary = "List Users by Role", description = "List users filtered by role")
    public ResponseEntity<ApiResponse<List<UserResponse>>> listUsersByRole(@PathVariable UserRole role) {
        log.debug("List users by role request: {}", role);

        try {
            List<UserResponse> users = userService.listUsersByRole(role);
            return ResponseEntity.ok(ApiResponse.success(users, "Users retrieved successfully"));

        } catch (Exception e) {
            log.error("Failed to list users by role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to list users: " + e.getMessage()));
        }
    }

    @GetMapping("/active")
    @Operation(summary = "List Active Users", description = "List all active users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> listActiveUsers() {
        log.debug("List active users request");

        try {
            List<UserResponse> users = userService.listActiveUsers();
            return ResponseEntity.ok(ApiResponse.success(users, "Active users retrieved successfully"));

        } catch (Exception e) {
            log.error("Failed to list active users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to list users: " + e.getMessage()));
        }
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update User", description = "Update user details")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("Update user request for ID: {}", userId);

        try {
            UserResponse user = userService.updateUser(userId, request);
            return ResponseEntity.ok(ApiResponse.success(user, "User updated successfully"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to update user: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete User", description = "Delete user permanently")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String userId) {
        log.info("Delete user request for ID: {}", userId);

        try {
            userService.deleteUser(userId);
            return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to delete user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to delete user: " + e.getMessage()));
        }
    }

    @PostMapping("/{userId}/deactivate")
    @Operation(summary = "Deactivate User", description = "Deactivate user (soft delete)")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(@PathVariable String userId) {
        log.info("Deactivate user request for ID: {}", userId);

        try {
            UserResponse user = userService.deactivateUser(userId);
            return ResponseEntity.ok(ApiResponse.success(user, "User deactivated successfully"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to deactivate user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to deactivate user: " + e.getMessage()));
        }
    }

    @PostMapping("/{userId}/activate")
    @Operation(summary = "Activate User", description = "Activate a deactivated user")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(@PathVariable String userId) {
        log.info("Activate user request for ID: {}", userId);

        try {
            UserResponse user = userService.activateUser(userId);
            return ResponseEntity.ok(ApiResponse.success(user, "User activated successfully"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to activate user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to activate user: " + e.getMessage()));
        }
    }
}
