package com.pulsepass.controller;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.services.AuthorizationService;
import com.pulsepass.services.TicketService;
import com.pulsepass.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TicketController {

    private final TicketService service;

    public TicketController(TicketService service, AuthorizationService authorizationService, UserService userService) {
        this.service = service;
        this.authorizationService = authorizationService;
        this.userService = userService;
    }

    @PostMapping("/api/tickets")
    public ResponseEntity<TicketResponse> purchase(@Valid @RequestBody PurchaseTicketRequest request) {
        TicketResponse response = service.purchase(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/tickets/{ticketCode}")
    public ResponseEntity<TicketResponse> findByCode(@PathVariable String ticketCode) {
        return ResponseEntity.ok(service.findByCode(ticketCode));
    }

    @GetMapping("/api/tickets/by-user")
    public ResponseEntity<List<TicketResponse>> findByUserEmail(@RequestParam String email) {
        return ResponseEntity.ok(service.findByUserEmail(email));
    }

    @GetMapping("/api/events/{eventCode}/tickets/paid")
    public ResponseEntity<List<TicketResponse>> findPaidTicketsByEvent(@PathVariable String eventCode) {
        return ResponseEntity.ok(service.findPaidTicketsByEvent(eventCode));
    }

    @PatchMapping("/api/tickets/{ticketCode}/cancel")
    public ResponseEntity<TicketResponse> cancel(@PathVariable String ticketCode) {
        return ResponseEntity.ok(service.cancel(ticketCode));
    }

    @PatchMapping("/api/tickets/{ticketCode}/use")
    public ResponseEntity<TicketResponse> markAsUsed(@PathVariable String ticketCode) {
        return ResponseEntity.ok(service.markAsUsed(ticketCode));
    }
    // agrega al constructor:
    private final AuthorizationService authorizationService;
    private final UserService userService; // para resolver email -> id del principal

    @PostMapping("/api/tickets")
    public ResponseEntity<TicketResponse> purchase(@Valid @RequestBody PurchaseTicketRequest request,
                                                   @AuthenticationPrincipal SecurityPrincipal principal) {
        if (!request.userEmail().equalsIgnoreCase(principal.getEmail())) {
            throw new com.pulsepass.exception.ForbiddenOperationException("You can only purchase tickets for yourself");
        }
        TicketResponse response = service.purchase(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/tickets/{ticketCode}")
    public ResponseEntity<TicketResponse> findByCode(@PathVariable String ticketCode,
                                                     @AuthenticationPrincipal SecurityPrincipal principal) {
        TicketResponse response = service.findByCode(ticketCode);
        Long ownerId = userService.findByEmail(response.userEmail()).id();
        authorizationService.requireOwnerOrAdmin(principal, ownerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/tickets/by-user")
    public ResponseEntity<List<TicketResponse>> findByUserEmail(@RequestParam String email,
                                                                @AuthenticationPrincipal SecurityPrincipal principal) {
        Long ownerId = userService.findByEmail(email).id();
        authorizationService.requireOwnerOrAdmin(principal, ownerId);
        return ResponseEntity.ok(service.findByUserEmail(email));
    }

    @PatchMapping("/api/tickets/{ticketCode}/cancel")
    public ResponseEntity<TicketResponse> cancel(@PathVariable String ticketCode,
                                                 @AuthenticationPrincipal SecurityPrincipal principal) {
        TicketResponse existing = service.findByCode(ticketCode);
        Long ownerId = userService.findByEmail(existing.userEmail()).id();
        authorizationService.requireOwnerOrAdmin(principal, ownerId);
        return ResponseEntity.ok(service.cancel(ticketCode));
    }
}