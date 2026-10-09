package com.pulsepass.controller;

import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.services.VenueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/venues")
public class VenueController {

    private final VenueService service;

    public VenueController(VenueService service) {
        this.service = service;
    }

    @GetMapping("/{code}")
    public ResponseEntity<VenueResponse> findByCode(@PathVariable String code) {
        return ResponseEntity.ok(service.findByCode(code));
    }

    @GetMapping("/active")
    public ResponseEntity<List<VenueResponse>> findActiveVenues() {
        return ResponseEntity.ok(service.findActiveVenues());
    }
}
