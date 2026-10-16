package com.pulsepass.services.impl;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.domain.enums.TicketType;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.services.TicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Service
public class TicketServiceImpl implements TicketService {

    private static final BigDecimal BASE_PRICE = new BigDecimal("100000");
    private static final BigDecimal STUDENT_FACTOR = new BigDecimal("0.70");
    private static final BigDecimal VIP_FACTOR = new BigDecimal("2.00");
    private static final BigDecimal BACKSTAGE_FACTOR = new BigDecimal("3.00");

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;

    public TicketServiceImpl(UserRepository userRepository,
                             EventRepository eventRepository,
                             TicketRepository ticketRepository,
                             TicketMapper ticketMapper) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.ticketMapper = ticketMapper;
    }

    @Override
    @Transactional
    public TicketResponse purchase(PurchaseTicketRequest request) {
        // BR-TICKET-001
        User user = userRepository.findByEmailIgnoreCase(request.userEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userEmail()));

        // BR-TICKET-002
        if (!user.isActive()) {
            throw new BusinessRuleException("Inactive user cannot purchase tickets: " + request.userEmail());
        }

        // BR-TICKET-003
        Event event = eventRepository.findByEventCode(request.eventCode())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + request.eventCode()));

        // BR-TICKET-004
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException("Tickets can only be purchased for PUBLISHED events: " + request.eventCode());
        }

        // BR-TICKET-005
        if (!event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot purchase tickets for a past event: " + request.eventCode());
        }

        // BR-TICKET-006
        if (event.getMinimumAge() != null && event.getMinimumAge() > 0) {
            if (user.getProfile() == null || user.getProfile().getBirthDate() == null) {
                throw new BusinessRuleException("User does not meet minimum age requirement");
            }
            int ageAtEvent = Period.between(user.getProfile().getBirthDate(), event.getEventDate().toLocalDate())
                    .getYears();
            if (ageAtEvent < event.getMinimumAge()) {
                throw new BusinessRuleException("User does not meet minimum age requirement");
            }
        }

        // BR-TICKET-007
        long paidTickets = ticketRepository.countByEventEventCodeAndStatus(event.getEventCode(), TicketStatus.PAID);
        if (paidTickets >= event.getVenue().getCapacity()) {
            throw new BusinessRuleException("Event has no remaining capacity: " + request.eventCode());
        }

        BigDecimal price = computePrice(request.type()); // BR-TICKET-009

        Ticket ticket = new Ticket();
        ticket.setTicketCode("TCK-" + UUID.randomUUID());
        ticket.setType(request.type());
        ticket.setPrice(price);
        ticket.setStatus(TicketStatus.PAID);
        ticket.setPurchaseDate(LocalDateTime.now());
        ticket.setUser(user);
        ticket.setEvent(event);

        Ticket saved = ticketRepository.save(ticket);

        // BR-TICKET-008: misma transacción
        long paidAfterPurchase = paidTickets + 1;
        if (paidAfterPurchase == event.getVenue().getCapacity()) {
            event.setStatus(EventStatus.SOLD_OUT);
            eventRepository.save(event);
        }

        return ticketMapper.toResponse(saved);
    }

    /**
     * Estrategia de precio encapsulada (BR-TICKET-009):
     * GENERAL = precio base, STUDENT = descuento, VIP y BACKSTAGE = multiplicadores.
     */
    private BigDecimal computePrice(TicketType type) {
        return switch (type) {
            case GENERAL -> BASE_PRICE;
            case STUDENT -> BASE_PRICE.multiply(STUDENT_FACTOR);
            case VIP -> BASE_PRICE.multiply(VIP_FACTOR);
            case BACKSTAGE -> BASE_PRICE.multiply(BACKSTAGE_FACTOR);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse findByCode(String ticketCode) {
        return ticketRepository.findByTicketCode(ticketCode)
                .map(ticketMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findByUserEmail(String email) {
        return ticketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(email)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findPaidTicketsByEvent(String eventCode) {
        return ticketRepository.findByEvent_EventCodeAndStatus(eventCode, TicketStatus.PAID)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TicketResponse cancel(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketCode));

        // BR-TICKET-010 / BR-TICKET-011
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException("Only a PAID ticket can be cancelled: " + ticketCode);
        }

        // BR-TICKET-012
        if (LocalDateTime.now().isAfter(ticket.getEvent().getEventDate())) {
            throw new BusinessRuleException("Cannot cancel a ticket after the event date: " + ticketCode);
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        Ticket saved = ticketRepository.save(ticket);
        return ticketMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TicketResponse markAsUsed(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketCode));

        // BR-TICKET-013 / BR-TICKET-014
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException("Only a PAID ticket can be marked as used: " + ticketCode);
        }

        ticket.setStatus(TicketStatus.USED);
        Ticket saved = ticketRepository.save(ticket);
        return ticketMapper.toResponse(saved);
    }
}
