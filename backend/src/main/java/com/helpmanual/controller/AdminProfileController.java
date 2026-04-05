package com.helpmanual.controller;

import com.helpmanual.dto.request.ChangePasswordRequest;
import com.helpmanual.dto.request.UpdateProfileRequest;
import com.helpmanual.dto.response.UserResponse;
import com.helpmanual.entity.User;
import com.helpmanual.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/profile")
public class AdminProfileController {

    private final UserService userService;

    public AdminProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        User user = userService.findById(userId);
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                                       Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        User user = userService.updateProfile(userId, request.displayName());
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                               Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        userService.changePassword(userId, request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}
