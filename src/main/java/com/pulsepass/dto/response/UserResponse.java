package com.pulsepass.dto.response;

public record UserResponse(
        Long id,
        String username,
        String email,
        boolean active,
        String firstName,
        String lastName
) {}