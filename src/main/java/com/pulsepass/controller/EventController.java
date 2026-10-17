package com.pulsepass.controller;

import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.services.AuthorizationService;
import com.pulsepass.services.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService service;
    private final AuthorizationService authorizationService;

    public EventController(EventService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        EventResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{eventCode}")
    public ResponseEntity<EventResponse> findByCode(@PathVariable String eventCode,
                                                    @AuthenticationPrincipal SecurityPrincipal principal) {
        EventResponse response = service.findByCode(eventCode);
        if (response.status() != EventStatus.PUBLISHED) {
            boolean staffOrAdmin = principal != null &&
                    (authorizationService.hasRole(principal, "STAFF")
                            || authorizationService.hasRole(principal, "ADMIN"));
            if (!staffOrAdmin) {
                throw new ResourceNotFoundException("Event not found: " + eventCode);
            }
        }
        return ResponseEntity.ok(response);
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