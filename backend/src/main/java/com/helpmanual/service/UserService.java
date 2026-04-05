package com.helpmanual.service;

import com.helpmanual.entity.User;
import com.helpmanual.entity.enums.Role;
import com.helpmanual.entity.enums.UserStatus;
import com.helpmanual.exception.BadRequestException;
import com.helpmanual.exception.ResourceNotFoundException;
import com.helpmanual.exception.UnauthorizedException;
import com.helpmanual.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int GENERATED_PASSWORD_LENGTH = 12;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User authenticate(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is disabled");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        return user;
    }

    @Transactional
    public UserWithPassword createUser(String username, String displayName, Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username already exists");
        }

        String generatedPassword = generateRandomPassword();

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(generatedPassword));
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setForcePasswordChange(true);
        user.setStatus(UserStatus.ACTIVE);

        User saved = userRepository.save(user);
        return new UserWithPassword(saved, generatedPassword);
    }

    @Transactional
    public User createAdmin(String username, String password, String displayName) {
        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(displayName);
        user.setRole(Role.ADMIN);
        user.setForcePasswordChange(false);
        user.setStatus(UserStatus.ACTIVE);

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, String displayName, Role role) {
        User user = findById(id);
        if (displayName != null) {
            user.setDisplayName(displayName);
        }
        if (role != null) {
            user.setRole(role);
        }
        return userRepository.save(user);
    }

    @Transactional
    public User toggleStatus(Long id) {
        User user = findById(id);
        if (user.getStatus() == UserStatus.ACTIVE) {
            user.setStatus(UserStatus.DISABLED);
        } else {
            user.setStatus(UserStatus.ACTIVE);
        }
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = findById(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setForcePasswordChange(false);
        userRepository.save(user);
    }

    @Transactional
    public String resetPassword(Long userId) {
        User user = findById(userId);
        String newPassword = generateRandomPassword();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setForcePasswordChange(true);
        userRepository.save(user);
        return newPassword;
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public boolean hasAnyUsers() {
        return userRepository.count() > 0;
    }

    @Transactional
    public User updateProfile(Long userId, String displayName) {
        User user = findById(userId);
        if (displayName != null) {
            user.setDisplayName(displayName);
        }
        return userRepository.save(user);
    }

    private String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(GENERATED_PASSWORD_LENGTH);
        for (int i = 0; i < GENERATED_PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    public record UserWithPassword(User user, String generatedPassword) {}
}
