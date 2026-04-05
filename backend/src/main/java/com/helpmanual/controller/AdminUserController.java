package com.helpmanual.controller;

import com.helpmanual.dto.request.CreateUserRequest;
import com.helpmanual.dto.request.UpdateUserRequest;
import com.helpmanual.dto.response.UserResponse;
import com.helpmanual.dto.response.UserWithPasswordResponse;
import com.helpmanual.entity.User;
import com.helpmanual.entity.enums.Role;
import com.helpmanual.service.OperationLogService;
import com.helpmanual.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;
    private final OperationLogService operationLogService;

    public AdminUserController(UserService userService,
                               OperationLogService operationLogService) {
        this.userService = userService;
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> list() {
        return ResponseEntity.ok(
                userService.findAll().stream()
                        .map(UserResponse::fromEntity)
                        .toList()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(UserResponse.fromEntity(userService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<UserWithPasswordResponse> create(@Valid @RequestBody CreateUserRequest request,
                                                            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Role role = Role.valueOf(request.role().toUpperCase());

        UserService.UserWithPassword result = userService.createUser(
                request.username(), request.displayName(), role);

        operationLogService.log(userId, "CREATE", "USER", result.user().getId(),
                "Created user: " + result.user().getUsername());

        return ResponseEntity.ok(new UserWithPasswordResponse(
                UserResponse.fromEntity(result.user()),
                result.generatedPassword()
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody UpdateUserRequest request,
                                                Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Role role = request.role() != null ? Role.valueOf(request.role().toUpperCase()) : null;

        User user = userService.updateUser(id, request.displayName(), role);

        operationLogService.log(userId, "UPDATE", "USER", user.getId(),
                "Updated user: " + user.getUsername());

        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<UserResponse> toggleStatus(@PathVariable Long id,
                                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        User user = userService.toggleStatus(id);

        operationLogService.log(userId, "TOGGLE_STATUS", "USER", user.getId(),
                "Toggled user status: " + user.getUsername() + " -> " + user.getStatus());

        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@PathVariable Long id,
                                                              Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String newPassword = userService.resetPassword(id);

        operationLogService.log(userId, "RESET_PASSWORD", "USER", id, "Reset user password");

        return ResponseEntity.ok(Map.of("generatedPassword", newPassword));
    }
}
