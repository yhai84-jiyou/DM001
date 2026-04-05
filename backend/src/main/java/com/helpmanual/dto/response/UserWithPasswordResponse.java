package com.helpmanual.dto.response;

public record UserWithPasswordResponse(
        UserResponse user,
        String generatedPassword
) {}
