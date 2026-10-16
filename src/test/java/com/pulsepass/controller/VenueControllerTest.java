package com.pulsepass.controller;

import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.services.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VenueController.class)
@Import(GlobalExceptionHandler.class)
class VenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VenueService service;

    private VenueResponse venue(String code) {
        return new VenueResponse(1L, code, "Marina Convention Center", "Santa Marta", "Calle 1", 3, true);
    }

    // TEST-CTRL-VEN-001
    @Test
    void shouldReturnVenueByCode() throws Exception {
        when(service.findByCode("VEN-SMR-01")).thenReturn(venue("VEN-SMR-01"));

        mockMvc.perform(get("/api/venues/{code}", "VEN-SMR-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VEN-SMR-01"))
                .andExpect(jsonPath("$.city").value("Santa Marta"));

        verify(service).findByCode("VEN-SMR-01");
    }

    // TEST-CTRL-VEN-002
    @Test
    void shouldReturn404WhenVenueDoesNotExist() throws Exception {
        when(service.findByCode("VEN-999"))
                .thenThrow(new ResourceNotFoundException("Venue not found: VEN-999"));

        mockMvc.perform(get("/api/venues/{code}", "VEN-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Venue not found: VEN-999"))
                .andExpect(jsonPath("$.details").isMap());
    }

    // TEST-CTRL-VEN-003
    @Test
    void shouldReturnActiveVenues() throws Exception {
        when(service.findActiveVenues()).thenReturn(List.of(venue("VEN-SMR-01")));

        mockMvc.perform(get("/api/venues/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(service).findActiveVenues();
    }
}