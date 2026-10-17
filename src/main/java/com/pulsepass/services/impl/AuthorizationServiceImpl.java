package com.pulsepass.service.impl;

import com.pulsepass.exception.ForbiddenOperationException;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.services.AuthorizationService;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationServiceImpl implements AuthorizationService {

    @Override
    public boolean isOwnerOrAdmin(SecurityPrincipal principal, Long resourceOwnerId) {
        if (principal == null || resourceOwnerId == null) {
            return false;
        }
        return hasRole(principal, "ADMIN") || principal.getUserId().equals(resourceOwnerId);
    }

    @Override
    public boolean hasRole(SecurityPrincipal principal, String role) {
        if (principal == null) {
            return false;
        }
        return principal.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    @Override
    public void requireOwnerOrAdmin(SecurityPrincipal principal, Long resourceOwnerId) {
        if (!isOwnerOrAdmin(principal, resourceOwnerId)) {
            throw new ForbiddenOperationException("You do not have permission to access this resource");
        }
    }
}