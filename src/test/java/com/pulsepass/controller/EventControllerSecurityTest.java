package com.pulsepass.controller;

import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.security.RestAccessDeniedHandler;
import com.pulsepass.security.RestAuthenticationEntryPoint;
import com.pulsepass.security.SecurityConfig;
import com.pulsepass.services.AuthorizationService;
import com.pulsepass.services.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        com.pulsepass.exception.GlobalExceptionHandler.class})
class EventControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService service;

    @MockitoBean
    private AuthorizationService authorizationService;

    @MockitoBean
    private com.pulsepass.security.JwtService jwtService;

    @MockitoBean
    private com.pulsepass.security.CustomUserDetailsService userDetailsService;

    private EventResponse published() {
        return new EventResponse(1L, "CMF-2026", "Caribbean Music Fest 2026", "desc",
                EventCategory.MUSIC, EventStatus.PUBLISHED, LocalDateTime.now().plusMonths(2),
                18, "VEN-SMR-01", "Marina Convention Center", Set.of());
    }

    // T-SEC-001
    @Test
    void anonymousCanListPublishedEvents() throws Exception {
        when(service.findPublishedEvents()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk());
    }

    // T-SEC-002
    @Test
    void anonymousCannotCreateEvent() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        verify(service, never()).create(any());
    }

    // T-SEC-003
    @Test
    @WithMockUser(roles = "USER")
    void userCannotCreateEvent() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "eventCode": "CMF-2026", "name": "n", "category": "MUSIC",
                                    "eventDate": "2026-12-01T20:00:00", "minimumAge": 18, "venueCode": "VEN-SMR-01"
                                }
                                """))
                .andExpect(status().isForbidden());

        verify(service, never()).create(any());
    }

    // T-SEC-004
    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateEvent() throws Exception {
        when(service.create(any(CreateEventRequest.class))).thenReturn(published());

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "eventCode": "CMF-2026", "name": "n", "category": "MUSIC",
                                    "eventDate": "2026-12-01T20:00:00", "minimumAge": 18, "venueCode": "VEN-SMR-01"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    // T-SEC-014
    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanPublishEvent() throws Exception {
        when(service.publish("CMF-2026")).thenReturn(published());

        mockMvc.perform(patch("/api/events/{code}/publish", "CMF-2026"))
                .andExpect(status().isOk());
    }

    // UC-SEC-08
    @Test
    @WithMockUser(roles = "USER")
    void userCannotPublishEvent() throws Exception {
        mockMvc.perform(patch("/api/events/{code}/publish", "CMF-2026"))
                .andExpect(status().isForbidden());

        verify(service, never()).publish(anyString());
    }
}