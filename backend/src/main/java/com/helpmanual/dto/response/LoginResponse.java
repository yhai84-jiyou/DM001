package com.helpmanual.dto.response;

public record LoginResponse(
        String token,
        UserResponse user
) {}
