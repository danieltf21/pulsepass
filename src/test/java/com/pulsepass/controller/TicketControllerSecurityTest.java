package com.pulsepass.controller;

import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.domain.enums.TicketType;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.security.*;
import com.pulsepass.services.impl.AuthorizationServiceImpl;
import com.pulsepass.services.TicketService;
import com.pulsepass.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, GlobalExceptionHandler.class, AuthorizationServiceImpl.class})
class TicketControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private TicketService service;
    @MockitoBean private UserService userService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService userDetailsService;

    private final SecurityPrincipal andrea = new SecurityPrincipal(1L, "andrea", "andrea@email.com", "hash", true, Set.of("USER"));
    private final SecurityPrincipal laura = new SecurityPrincipal(2L, "laura", "laura@email.com", "hash", true, Set.of("STAFF"));

    private TicketResponse ticket(String code, String ownerEmail, TicketStatus status) {
        return new TicketResponse(1L, code, TicketType.VIP, new BigDecimal("250000"), status,
                LocalDateTime.now(), ownerEmail, "CMF-2026", "Caribbean Music Fest 2026");
    }

    // T-SEC-010
    @Test
    void userCannotPurchaseForSomeoneElse() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .with(user(andrea))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userEmail": "carlos@email.com", "eventCode": "CMF-2026", "type": "GENERAL"}
                                """))
                .andExpect(status().isForbidden());

        verify(service, never()).purchase(any());
    }

    // UC-SEC-04
    @Test
    void userCanPurchaseForThemselves() throws Exception {
        when(service.purchase(any())).thenReturn(ticket("TCK-1", "andrea@email.com", TicketStatus.PAID));

        mockMvc.perform(post("/api/tickets")
                        .with(user(andrea))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userEmail": "andrea@email.com", "eventCode": "CMF-2026", "type": "VIP"}
                                """))
                .andExpect(status().isCreated());
    }

    // T-SEC-009
    @Test
    void userCannotReadSomeoneElsesTicket() throws Exception {
        when(service.findByCode("TCK-1")).thenReturn(ticket("TCK-1", "carlos@email.com", TicketStatus.PAID));
        when(userService.findByEmail("carlos@email.com")).thenReturn(
                new UserResponse(2L, "carlos", "carlos@email.com", true, "Carlos", "Perez"));

        mockMvc.perform(get("/api/tickets/{code}", "TCK-1").with(user(andrea)))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCanReadSomeoneElsesTicket() throws Exception {
        assertCanReadTicket(laura);
    }

    @Test
    void adminCanReadSomeoneElsesTicket() throws Exception {
        assertCanReadTicket(new SecurityPrincipal(3L, "admin", "admin@email.com", "hash", true, Set.of("ADMIN")));
    }

    @Test
    void ownerCanReadTheirTicket() throws Exception {
        assertCanReadTicket(andrea);
    }

    private void assertCanReadTicket(SecurityPrincipal principal) throws Exception {
        when(service.findByCode("TCK-1")).thenReturn(ticket("TCK-1", "andrea@email.com", TicketStatus.PAID));
        when(userService.findByEmail("andrea@email.com")).thenReturn(
                new UserResponse(1L, "andrea", "andrea@email.com", true, "Andrea", "Perez"));

        mockMvc.perform(get("/api/tickets/{code}", "TCK-1").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.ticketCode").value("TCK-1"));
    }

    @Test
    void anonymousCannotReadTicket() throws Exception {
        mockMvc.perform(get("/api/tickets/{code}", "TCK-1"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service, userService);
    }

    @Test
    void staffCannotListSomeoneElsesTicketsByUser() throws Exception {
        when(userService.findByEmail("andrea@email.com")).thenReturn(
                new UserResponse(1L, "andrea", "andrea@email.com", true, "Andrea", "Perez"));
        mockMvc.perform(get("/api/tickets/by-user").param("email", "andrea@email.com").with(user(laura)))
                .andExpect(status().isForbidden());
        verify(service, never()).findByUserEmail(anyString());
    }

    @Test
    void staffCannotCancelTicket() throws Exception {
        mockMvc.perform(patch("/api/tickets/{code}/cancel", "TCK-1").with(user(laura)))
                .andExpect(status().isForbidden());
        verify(service, never()).cancel(anyString());
    }

    @Test
    void staffCannotPurchaseTicket() throws Exception {
        mockMvc.perform(post("/api/tickets").with(user(laura))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userEmail": "laura@email.com", "eventCode": "CMF-2026", "type": "VIP"}
                                """))
                .andExpect(status().isForbidden());
        verify(service, never()).purchase(any());
    }

    // T-SEC-012
    @Test
    void userCannotMarkTicketAsUsed() throws Exception {
        mockMvc.perform(patch("/api/tickets/{code}/use", "TCK-1").with(user(andrea)))
                .andExpect(status().isForbidden());

        verify(service, never()).markAsUsed(anyString());
    }

    // T-SEC-013
    @Test
    void staffCanMarkTicketAsUsed() throws Exception {
        when(service.markAsUsed("TCK-1")).thenReturn(ticket("TCK-1", "andrea@email.com", TicketStatus.USED));

        mockMvc.perform(patch("/api/tickets/{code}/use", "TCK-1").with(user(laura)))
                .andExpect(status().isOk());
    }

    // T-SEC-004 (variante Ticket)
    @Test
    @WithMockUser(roles = "STAFF")
    void staffCanListPaidTicketsByEvent() throws Exception {
        when(service.findPaidTicketsByEvent("CMF-2026")).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/events/{code}/tickets/paid", "CMF-2026"))
                .andExpect(status().isOk());
    }
}