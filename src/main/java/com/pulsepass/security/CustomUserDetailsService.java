package com.pulsepass.security;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final UserRoleRepository userRoleRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    UserCredentialRepository userCredentialRepository,
                                    UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        return loadByIdentifier(identifier);
    }

    public UserDetails loadByUserId(Long userId) throws UsernameNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
        return toPrincipal(user);
    }

    private UserDetails loadByIdentifier(String identifier) {
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmailIgnoreCase(identifier))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));
        return toPrincipal(user);
    }

    private UserDetails toPrincipal(User user) {
        UserCredential credential = userCredentialRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("No credentials for user: " + user.getId()));

        Set<String> roles = userRoleRepository.findByUserId(user.getId()).stream()
                .map(role -> role.getRole().name())
                .collect(Collectors.toSet());

        return new SecurityPrincipal(user.getId(), user.getUsername(), user.getEmail(),
                credential.getPasswordHash(), user.isActive(), roles);
    }
}