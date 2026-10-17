package com.pulsepass.dto.response;

import java.util.Set;

public record AuthenticatedUserResponse(
        Long id,
        String username,
        String email,
        Set<String> roles
) {}