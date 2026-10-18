package com.pulsepass.services.impl;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.domain.enums.RoleName;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.security.JwtService;
import com.pulsepass.services.AuthService;
import com.pulsepass.services.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthServiceImpl(UserService userService,
                           UserRepository userRepository,
                           UserCredentialRepository userCredentialRepository,
                           UserRoleRepository userRoleRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        UserResponse userResponse = userService.register(request);

        User user = userRepository.findById(userResponse.id())
                .orElseThrow(() -> new ResourceNotFoundException("User not found after registration"));

        UserCredential credential = new UserCredential();
        credential.setUserId(user.getId());
        credential.setUser(user);
        credential.setPasswordHash(passwordEncoder.encode(request.password()));
        userCredentialRepository.save(credential);

        userRoleRepository.save(new UserRole(user.getId(), RoleName.USER));

        return userResponse;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.identifier(), request.password()));
        } catch (Exception ex) {
            throw new BadCredentialsException("Invalid username/email or password");
        }

        com.pulsepass.security.SecurityPrincipal principal =
                (com.pulsepass.security.SecurityPrincipal) authentication.getPrincipal();

        String token = jwtService.generateToken(principal.getUserId());
        return new AuthResponse(token, "Bearer", jwtService.getAccessTokenTtlSeconds());
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticatedUserResponse getAuthenticatedUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Set<String> roles = userRoleRepository.findByUserId(userId).stream()
                .map(role -> role.getRole().name())
                .collect(Collectors.toSet());

        return new AuthenticatedUserResponse(user.getId(), user.getUsername(), user.getEmail(), roles);
    }
}