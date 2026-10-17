package com.pulsepass.controller;

import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.ForbiddenOperationException;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.RestAccessDeniedHandler;
import com.pulsepass.security.RestAuthenticationEntryPoint;
import com.pulsepass.security.SecurityConfig;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.services.AuthService;
import com.pulsepass.services.AuthorizationService;
import com.pulsepass.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.util.Set;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, GlobalExceptionHandler.class})
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService service;

    @MockitoBean
    private AuthorizationService authorizationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private final SecurityPrincipal andrea =
            new SecurityPrincipal(1L, "andrea", "andrea@email.com", "hash", true, Set.of("USER"));

    private final SecurityPrincipal admin =
            new SecurityPrincipal(99L, "admin", "admin@email.com", "hash", true, Set.of("ADMIN"));

    private UserResponse user(Long id, String username, String email) {
        return new UserResponse(id, username, email, true, "Nombre", "Apellido");
    }

    // T-SEC-015
    @Test
    void userCannotReadSomeoneElsesProfileByEmail() throws Exception {
        when(service.findByEmail("carlos@email.com")).thenReturn(user(2L, "carlos", "carlos@email.com"));
        doThrow(new ForbiddenOperationException("forbidden"))
                .when(authorizationService).requireOwnerOrAdmin(any(), eq(2L));

        mockMvc.perform(get("/api/users/by-email").param("email", "carlos@email.com").with(SecurityMockMvcRequestPostProcessors.user(andrea)))
                .andExpect(status().isForbidden());
    }

    // UC-SEC-06
    @Test
    void userCanReadTheirOwnProfileByEmail() throws Exception {
        when(service.findByEmail("andrea@email.com")).thenReturn(user(1L, "andrea", "andrea@email.com"));

        mockMvc.perform(get("/api/users/by-email").param("email", "andrea@email.com").with(SecurityMockMvcRequestPostProcessors.user(andrea)))
                .andExpect(status().isOk());
    }

    // UC-SEC-07
    @Test
    void adminCanReadAnyProfileByEmail() throws Exception {
        when(service.findByEmail("carlos@email.com")).thenReturn(user(2L, "carlos", "carlos@email.com"));

        mockMvc.perform(get("/api/users/by-email").param("email", "carlos@email.com").with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void userCannotReadSomeoneElsesProfileByUsername() throws Exception {
        when(service.findByUsername("carlos")).thenReturn(user(2L, "carlos", "carlos@email.com"));
        doThrow(new ForbiddenOperationException("forbidden"))
                .when(authorizationService).requireOwnerOrAdmin(any(), eq(2L));

        mockMvc.perform(get("/api/users/by-username").param("username", "carlos").with(SecurityMockMvcRequestPostProcessors.user(andrea)))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanReadTheirOwnProfileByUsername() throws Exception {
        when(service.findByUsername("andrea")).thenReturn(user(1L, "andrea", "andrea@email.com"));

        mockMvc.perform(get("/api/users/by-username").param("username", "andrea").with(SecurityMockMvcRequestPostProcessors.user(andrea)))
                .andExpect(status().isOk());
    }

    // T-SEC-001 (variante User: registro sigue siendo público)
    @Test
    void anonymousCanRegister() throws Exception {
        // cubierto también por UserControllerTest normal; aquí solo confirmamos
        // que /api/users NO exige autenticación a nivel de SecurityConfig
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/users")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest()); // 400 por validación, no 401
    }
}