package com.pulsepass.services;

import com.pulsepass.security.SecurityPrincipal;

public interface AuthorizationService {

    boolean isOwnerOrAdmin(SecurityPrincipal principal, Long resourceOwnerId);

    boolean hasRole(SecurityPrincipal principal, String role);

    void requireOwnerOrAdmin(SecurityPrincipal principal, Long resourceOwnerId);
}
