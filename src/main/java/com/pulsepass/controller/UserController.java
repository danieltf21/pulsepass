package com.pulsepass.controller;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.services.AuthService;
import com.pulsepass.services.AuthorizationService;
import com.pulsepass.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;
    private final UserService service;

    public UserController(AuthService authService, UserService service, AuthorizationService authorizationService) {
        this.authService = authService;
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/by-email")
    public ResponseEntity<UserResponse> findByEmail(@RequestParam String email) {
        return ResponseEntity.ok(service.findByEmail(email));
    }

    @GetMapping("/by-username")
    public ResponseEntity<UserResponse> findByUsername(@RequestParam String username) {
        return ResponseEntity.ok(service.findByUsername(username));
    }
    // agrega estas dependencias al constructor:
    private final AuthorizationService authorizationService;

    @GetMapping("/by-email")
    public ResponseEntity<UserResponse> findByEmail(@RequestParam String email,
                                                    @AuthenticationPrincipal SecurityPrincipal principal) {
        UserResponse response = service.findByEmail(email);
        authorizationService.requireOwnerOrAdmin(principal, response.id());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-username")
    public ResponseEntity<UserResponse> findByUsername(@RequestParam String username,
                                                       @AuthenticationPrincipal SecurityPrincipal principal) {
        UserResponse response = service.findByUsername(username);
        authorizationService.requireOwnerOrAdmin(principal, response.id());
        return ResponseEntity.ok(response);
    }
}