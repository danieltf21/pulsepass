package com.pulsepass.controller;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.services.ArtistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ArtistController.class)
@Import(GlobalExceptionHandler.class)
class ArtistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistService service;

    private ArtistResponse artist(Long id, String stageName) {
        return new ArtistResponse(id, stageName, "Colombia", "Electronic", true);
    }

    // TEST-CTRL-ART-001
    @Test
    void shouldReturnArtistById() throws Exception {
        when(service.findById(1L)).thenReturn(artist(1L, "Solar Beat"));

        mockMvc.perform(get("/api/artists/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageName").value("Solar Beat"));

        verify(service).findById(1L);
    }

    // TEST-CTRL-ART-002
    @Test
    void shouldReturn404WhenArtistIdDoesNotExist() throws Exception {
        when(service.findById(99L)).thenThrow(new ResourceNotFoundException("Artist not found: 99"));

        mockMvc.perform(get("/api/artists/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Artist not found: 99"));
    }

    // TEST-CTRL-ART-003
    @Test
    void shouldReturnArtistByStageName() throws Exception {
        when(service.findByStageName("Solar Beat")).thenReturn(artist(1L, "Solar Beat"));

        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageName").value("Solar Beat"));

        verify(service).findByStageName("Solar Beat");
    }

    // TEST-CTRL-ART-004
    @Test
    void shouldReturnActiveArtists() throws Exception {
        when(service.findActiveArtists()).thenReturn(List.of(artist(1L, "Solar Beat")));

        mockMvc.perform(get("/api/artists/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(service).findActiveArtists();
    }
}