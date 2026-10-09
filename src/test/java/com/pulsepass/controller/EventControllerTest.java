package com.pulsepass.controller;

import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.services.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@Import(GlobalExceptionHandler.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService service;

    private EventResponse event(String code, EventStatus status) {
        return new EventResponse(1L, code, "Caribbean Music Fest 2026", "desc",
                EventCategory.MUSIC, status, LocalDateTime.now().plusMonths(2),
                18, "VEN-SMR-01", "Marina Convention Center", Set.of());
    }

    // TEST-CTRL-EVT-001
    @Test
    void shouldCreateEvent() throws Exception {
        when(service.create(any(CreateEventRequest.class))).thenReturn(event("CMF-2026", EventStatus.DRAFT));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "eventCode": "CMF-2026",
                                    "name": "Caribbean Music Fest 2026",
                                    "description": "Festival de musica caribena",
                                    "category": "MUSIC",
                                    "eventDate": "2026-12-01T20:00:00",
                                    "minimumAge": 18,
                                    "venueCode": "VEN-SMR-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(service).create(any(CreateEventRequest.class));
    }

    // TEST-CTRL-EVT-002
    @Test
    void shouldReturn400WhenCreateRequestInvalid() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "eventCode": "",
                                    "name": "",
                                    "category": null,
                                    "eventDate": null,
                                    "minimumAge": null,
                                    "venueCode": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.eventCode").exists())
                .andExpect(jsonPath("$.details.venueCode").exists());

        verify(service, never()).create(any());
    }

    // TEST-CTRL-EVT-003
    @Test
    void shouldReturnEventByCode() throws Exception {
        when(service.findByCode("CMF-2026")).thenReturn(event("CMF-2026", EventStatus.PUBLISHED));

        mockMvc.perform(get("/api/events/{code}", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));

        verify(service).findByCode("CMF-2026");
    }

    // TEST-CTRL-EVT-004
    @Test
    void shouldReturn404WhenEventDoesNotExist() throws Exception {
        when(service.findByCode("NOPE")).thenThrow(new ResourceNotFoundException("Event not found: NOPE"));

        mockMvc.perform(get("/api/events/{code}", "NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Event not found: NOPE"));
    }

    // TEST-CTRL-EVT-005
    @Test
    void shouldReturnPublishedEvents() throws Exception {
        EventSummaryResponse summary = new EventSummaryResponse("CMF-2026", "Caribbean Music Fest 2026",
                EventStatus.PUBLISHED, LocalDateTime.now().plusMonths(2), "Marina Convention Center");
        when(service.findPublishedEvents()).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));

        verify(service).findPublishedEvents();
    }

    // TEST-CTRL-EVT-006
    @Test
    void shouldPublishEvent() throws Exception {
        when(service.publish("CMF-2026")).thenReturn(event("CMF-2026", EventStatus.PUBLISHED));

        mockMvc.perform(patch("/api/events/{code}/publish", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(service).publish("CMF-2026");
    }

    // TEST-CTRL-EVT-007
    @Test
    void shouldReturn409WhenPublishInvalid() throws Exception {
        when(service.publish("CMF-2026")).thenThrow(new BusinessRuleException("Only a DRAFT event can be published"));

        mockMvc.perform(patch("/api/events/{code}/publish", "CMF-2026"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Only a DRAFT event can be published"));
    }

    // TEST-CTRL-EVT-008
    @Test
    void shouldAddArtistToEvent() throws Exception {
        when(service.addArtist("CMF-2026", 1L)).thenReturn(event("CMF-2026", EventStatus.DRAFT));

        mockMvc.perform(post("/api/events/{code}/artists/{artistId}", "CMF-2026", 1L))
                .andExpect(status().isOk());

        verify(service).addArtist("CMF-2026", 1L);
    }

    // TEST-CTRL-EVT-009
    @Test
    void shouldFindEventsByArtist() throws Exception {
        EventSummaryResponse summary = new EventSummaryResponse("CMF-2026", "Caribbean Music Fest 2026",
                EventStatus.PUBLISHED, LocalDateTime.now().plusMonths(2), "Marina Convention Center");
        when(service.findByArtist("Solar Beat")).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/events/by-artist").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(service).findByArtist("Solar Beat");
    }
}