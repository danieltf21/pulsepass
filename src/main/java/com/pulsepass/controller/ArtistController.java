package com.pulsepass.controller;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.services.ArtistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/artists")
public class ArtistController {

    private final ArtistService service;

    public ArtistController(ArtistService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArtistResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/by-stage-name")
    public ResponseEntity<ArtistResponse> findByStageName(@RequestParam String stageName) {
        return ResponseEntity.ok(service.findByStageName(stageName));
    }

    @GetMapping("/active")
    public ResponseEntity<List<ArtistResponse>> findActiveArtists() {
        return ResponseEntity.ok(service.findActiveArtists());
    }
}