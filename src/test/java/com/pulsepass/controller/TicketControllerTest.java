package com.pulsepass.controller;

import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.domain.enums.TicketType;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.services.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketController.class)
@Import(GlobalExceptionHandler.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService service;

    private static final String VALID_BODY = """
            {
                "userEmail": "andrea@email.com",
                "eventCode": "CMF-2026",
                "type": "VIP"
            }
            """;

    private TicketResponse ticket(String code, TicketStatus status) {
        return new TicketResponse(1L, code, TicketType.VIP, new BigDecimal("250000"), status,
                LocalDateTime.now(), "andrea@email.com", "CMF-2026", "Caribbean Music Fest 2026");
    }

    // TEST-CTRL-TKT-001
    @Test
    void shouldPurchaseTicket() throws Exception {
        when(service.purchase(any(PurchaseTicketRequest.class))).thenReturn(ticket("TCK-0001", TicketStatus.PAID));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"));

        verify(service).purchase(any(PurchaseTicketRequest.class));
    }

    // TEST-CTRL-TKT-002
    @Test
    void shouldReturn400WhenPurchaseRequestInvalid() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userEmail": "",
                                    "eventCode": "",
                                    "type": null
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(service, never()).purchase(any());
    }

    // TEST-CTRL-TKT-003
    @Test
    void shouldReturn404WhenUserDoesNotExist() throws Exception {
        when(service.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found: andrea@email.com"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    // TEST-CTRL-TKT-004
    @Test
    void shouldReturn409WhenBusinessRuleViolated() throws Exception {
        when(service.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new BusinessRuleException("Event has no remaining capacity"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Event has no remaining capacity"));
    }

    // TEST-CTRL-TKT-005
    @Test
    void shouldFindTicketByCode() throws Exception {
        when(service.findByCode("TCK-0001")).thenReturn(ticket("TCK-0001", TicketStatus.PAID));

        mockMvc.perform(get("/api/tickets/{code}", "TCK-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value("TCK-0001"));

        verify(service).findByCode("TCK-0001");
    }

    // TEST-CTRL-TKT-006
    @Test
    void shouldFindTicketsByUserEmail() throws Exception {
        when(service.findByUserEmail("andrea@email.com")).thenReturn(List.of(ticket("TCK-0001", TicketStatus.PAID)));

        mockMvc.perform(get("/api/tickets/by-user").param("email", "andrea@email.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(service).findByUserEmail("andrea@email.com");
    }

    // TEST-CTRL-TKT-007
    @Test
    void shouldFindPaidTicketsByEvent() throws Exception {
        when(service.findPaidTicketsByEvent("CMF-2026")).thenReturn(List.of(ticket("TCK-0001", TicketStatus.PAID)));

        mockMvc.perform(get("/api/events/{code}/tickets/paid", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(service).findPaidTicketsByEvent("CMF-2026");
    }

    // TEST-CTRL-TKT-008
    @Test
    void shouldCancelTicket() throws Exception {
        when(service.cancel("TCK-0001")).thenReturn(ticket("TCK-0001", TicketStatus.CANCELLED));

        mockMvc.perform(patch("/api/tickets/{code}/cancel", "TCK-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(service).cancel("TCK-0001");
    }

    // TEST-CTRL-TKT-009
    @Test
    void shouldReturn409WhenCancelInvalid() throws Exception {
        when(service.cancel("TCK-0001")).thenThrow(new BusinessRuleException("Only a PAID ticket can be cancelled"));

        mockMvc.perform(patch("/api/tickets/{code}/cancel", "TCK-0001"))
                .andExpect(status().isConflict());
    }

    // TEST-CTRL-TKT-010
    @Test
    void shouldMarkTicketAsUsed() throws Exception {
        when(service.markAsUsed("TCK-0001")).thenReturn(ticket("TCK-0001", TicketStatus.USED));

        mockMvc.perform(patch("/api/tickets/{code}/use", "TCK-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USED"));

        verify(service).markAsUsed("TCK-0001");
    }

    // TEST-CTRL-TKT-011
    @Test
    void shouldReturn409WhenMarkAsUsedInvalid() throws Exception {
        when(service.markAsUsed("TCK-0001")).thenThrow(new BusinessRuleException("Only a PAID ticket can be marked as used"));

        mockMvc.perform(patch("/api/tickets/{code}/use", "TCK-0001"))
                .andExpect(status().isConflict());
    }
}