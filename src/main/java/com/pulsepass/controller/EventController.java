package com.pulsepass.controller;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.services.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService service;

    public EventController(EventService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        EventResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{eventCode}")
    public ResponseEntity<EventResponse> findByCode(@PathVariable String eventCode) {
        return ResponseEntity.ok(service.findByCode(eventCode));
    }

    @GetMapping("/published")
    public ResponseEntity<List<EventSummaryResponse>> findPublishedEvents() {
        return ResponseEntity.ok(service.findPublishedEvents());
    }

    @PatchMapping("/{eventCode}/publish")
    public ResponseEntity<EventResponse> publish(@PathVariable String eventCode) {
        return ResponseEntity.ok(service.publish(eventCode));
    }

    @PostMapping("/{eventCode}/artists/{artistId}")
    public ResponseEntity<EventResponse> addArtist(@PathVariable String eventCode, @PathVariable Long artistId) {
        return ResponseEntity.ok(service.addArtist(eventCode, artistId));
    }

    @GetMapping("/by-artist")
    public ResponseEntity<List<EventSummaryResponse>> findByArtist(@RequestParam String stageName) {
        return ResponseEntity.ok(service.findByArtist(stageName));
    }
}