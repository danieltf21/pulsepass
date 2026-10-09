package com.pulsepass.controller;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService service;

    private static final String VALID_BODY = """
            {
                "username": "andrea",
                "email": "andrea@email.com",
                "firstName": "Andrea",
                "lastName": "Gomez",
                "phone": "3001234567",
                "city": "Santa Marta",
                "birthDate": "2000-05-10"
            }
            """;

    private UserResponse user() {
        return new UserResponse(1L, "andrea", "andrea@email.com", true, "Andrea", "Gomez");
    }

    // TEST-CTRL-USR-001
    @Test
    void shouldRegisterUser() throws Exception {
        when(service.register(any(RegisterUserRequest.class))).thenReturn(user());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("andrea"));

        verify(service).register(any(RegisterUserRequest.class));
    }

    // TEST-CTRL-USR-002
    @Test
    void shouldReturn400WhenEmailInvalid() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("andrea@email.com", "not-an-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.email").exists());

        verify(service, never()).register(any());
    }

    // TEST-CTRL-USR-003
    @Test
    void shouldReturn409WhenUsernameDuplicated() throws Exception {
        when(service.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Username already exists: andrea"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already exists: andrea"));
    }

    // TEST-CTRL-USR-004
    @Test
    void shouldFindUserByEmail() throws Exception {
        when(service.findByEmail("andrea@email.com")).thenReturn(user());

        mockMvc.perform(get("/api/users/by-email").param("email", "andrea@email.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("andrea@email.com"));

        verify(service).findByEmail("andrea@email.com");
    }

    // TEST-CTRL-USR-005
    @Test
    void shouldFindUserByUsername() throws Exception {
        when(service.findByUsername("andrea")).thenReturn(user());

        mockMvc.perform(get("/api/users/by-username").param("username", "andrea"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("andrea"));

        verify(service).findByUsername("andrea");
    }

    @Test
    void shouldReturn404WhenUserNotFoundByEmail() throws Exception {
        when(service.findByEmail("nope@email.com"))
                .thenThrow(new ResourceNotFoundException("User not found: nope@email.com"));

        mockMvc.perform(get("/api/users/by-email").param("email", "nope@email.com"))
                .andExpect(status().isNotFound());
    }
}
