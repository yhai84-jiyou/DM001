package com.helpmanual.controller;

import com.helpmanual.dto.request.LoginRequest;
import com.helpmanual.dto.request.SetupRequest;
import com.helpmanual.dto.response.LoginResponse;
import com.helpmanual.dto.response.UserResponse;
import com.helpmanual.entity.User;
import com.helpmanual.exception.BadRequestException;
import com.helpmanual.exception.ResourceNotFoundException;
import com.helpmanual.security.LoginRateLimiter;
import com.helpmanual.service.DataInitializer;
import com.helpmanual.service.OperationLogService;
import com.helpmanual.service.UserService;
import com.helpmanual.util.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/admin/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final OperationLogService operationLogService;
    private final DataInitializer dataInitializer;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(UserService userService,
                          JwtTokenProvider jwtTokenProvider,
                          OperationLogService operationLogService,
                          DataInitializer dataInitializer,
                          LoginRateLimiter loginRateLimiter) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.operationLogService = operationLogService;
        this.dataInitializer = dataInitializer;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String rateLimitKey = request.username();

        if (loginRateLimiter.isBlocked(rateLimitKey)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Too many login attempts. Please try again later."));
        }

        try {
            User user = userService.authenticate(request.username(), request.password());
            loginRateLimiter.recordSuccess(rateLimitKey);

            String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole().name());

            operationLogService.log(user.getId(), "LOGIN", "USER", user.getId(), "User logged in");

            return ResponseEntity.ok(new LoginResponse(token, UserResponse.fromEntity(user)));
        } catch (Exception e) {
            loginRateLimiter.recordFailure(rateLimitKey);
            throw e;
        }
    }

    @PostMapping("/setup")
    public ResponseEntity<LoginResponse> setup(@Valid @RequestBody SetupRequest request) {
        if (userService.hasAnyUsers()) {
            throw new BadRequestException("Setup already completed. An admin user already exists.");
        }

        User admin = userService.createAdmin(request.username(), request.password(), request.displayName());

        if (request.seedData() != null && request.seedData()) {
            dataInitializer.seedSampleData(admin.getId());
        }

        String token = jwtTokenProvider.generateToken(admin.getId(), admin.getUsername(), admin.getRole().name());

        operationLogService.log(admin.getId(), "SETUP", "USER", admin.getId(), "Initial admin created");

        return ResponseEntity.ok(new LoginResponse(token, UserResponse.fromEntity(admin)));
    }

    @GetMapping("/setup-status")
    public ResponseEntity<Map<String, Boolean>> setupStatus() {
        boolean needSetup = !userService.hasAnyUsers();
        return ResponseEntity.ok(Map.of("needSetup", needSetup));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        User user = userService.findById(userId);
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
