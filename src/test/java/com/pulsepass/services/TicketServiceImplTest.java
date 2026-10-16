package com.pulsepass.services;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.domain.enums.TicketType;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.domain.Ticket;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.services.impl.TicketServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.eq;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private EventRepository eventRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private TicketMapper ticketMapper;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private Venue venue(int capacity) {
        Venue v = new Venue();
        v.setId(1L);
        v.setCode("VEN-SMR-01");
        v.setCapacity(capacity);
        v.setActive(true);
        return v;
    }

    private User activeUser(LocalDate birthDate) {
        User u = new User();
        u.setId(1L);
        u.setEmail("andrea@email.com");
        u.setActive(true);
        if (birthDate != null) {
            UserProfile profile = new UserProfile();
            profile.setBirthDate(birthDate);
            profile.setUser(u);
            u.setProfile(profile);
        }
        return u;
    }

    private Event event(EventStatus status, Venue venue, int minimumAge) {
        Event e = new Event();
        e.setId(1L);
        e.setEventCode("CMF-2026");
        e.setName("Caribbean Music Fest 2026");
        e.setStatus(status);
        e.setEventDate(LocalDateTime.now().plusMonths(2));
        e.setMinimumAge(minimumAge);
        e.setVenue(venue);
        return e;
    }

    private PurchaseTicketRequest request(TicketType type) {
        return new PurchaseTicketRequest("andrea@email.com", "CMF-2026", type);
    }

    // TEST-TICKET-001
    @Test
    void purchase_validRequest_createsPaidTicket() {
        Venue venue = venue(3);
        User user = activeUser(LocalDate.of(2000, 1, 1));
        Event event = event(EventStatus.PUBLISHED, venue, 18);
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(ticketRepository.countByEventEventCodeAndStatus("CMF-2026", TicketStatus.PAID)).thenReturn(0L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(mock(TicketResponse.class));

        ticketService.purchase(request(TicketType.GENERAL));

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TicketStatus.PAID);
        assertThat(captor.getValue().getPrice()).isGreaterThan(java.math.BigDecimal.ZERO);
    }

    // TEST-TICKET-002
    @Test
    void purchase_userNotFound_throwsResourceNotFound() {
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.purchase(request(TicketType.GENERAL)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ticketRepository, never()).save(any());
    }

    // TEST-TICKET-003
    @Test
    void purchase_inactiveUser_throwsBusinessRule() {
        User inactive = activeUser(LocalDate.of(2000, 1, 1));
        inactive.setActive(false);
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> ticketService.purchase(request(TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    // TEST-TICKET-004
    @Test
    void purchase_draftEvent_throwsBusinessRule() {
        Venue venue = venue(3);
        User user = activeUser(LocalDate.of(2000, 1, 1));
        Event draft = event(EventStatus.DRAFT, venue, 18);
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> ticketService.purchase(request(TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    // TEST-TICKET-005
    @Test
    void purchase_cancelledEvent_throwsBusinessRule() {
        Venue venue = venue(3);
        User user = activeUser(LocalDate.of(2000, 1, 1));
        Event cancelled = event(EventStatus.CANCELLED, venue, 18);
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> ticketService.purchase(request(TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    // TEST-TICKET-006 (Laura, 17 años, evento minimumAge=18)
    @Test
    void purchase_underMinimumAge_throwsBusinessRule() {
        Venue venue = venue(3);
        LocalDate birthDateFor17AtEvent = LocalDateTime.now().plusMonths(2).toLocalDate().minusYears(17);
        User minor = activeUser(birthDateFor17AtEvent);
        Event event = event(EventStatus.PUBLISHED, venue, 18);
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(minor));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> ticketService.purchase(request(TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    // TEST-TICKET-007
    @Test
    void purchase_noCapacityLeft_throwsBusinessRule() {
        Venue venue = venue(1);
        User user = activeUser(LocalDate.of(2000, 1, 1));
        Event event = event(EventStatus.PUBLISHED, venue, 18);
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(ticketRepository.countByEventEventCodeAndStatus("CMF-2026", TicketStatus.PAID)).thenReturn(1L);

        assertThatThrownBy(() -> ticketService.purchase(request(TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    // TEST-TICKET-008
    @Test
    void purchase_lastAvailableTicket_savesTicketAndMarksEventSoldOut() {
        Venue venue = venue(1);
        User user = activeUser(LocalDate.of(2000, 1, 1));
        Event event = event(EventStatus.PUBLISHED, venue, 18);
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(ticketRepository.countByEventEventCodeAndStatus("CMF-2026", TicketStatus.PAID)).thenReturn(0L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(mock(TicketResponse.class));

        ticketService.purchase(request(TicketType.GENERAL));

        verify(ticketRepository).save(any(Ticket.class));
        verify(ticketRepository).countByEventEventCodeAndStatus(eq("CMF-2026"), eq(TicketStatus.PAID));
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getStatus()).isEqualTo(EventStatus.SOLD_OUT);
    }

    private Ticket ticket(TicketStatus status, LocalDateTime eventDate) {
        Venue venue = venue(3);
        Event event = event(EventStatus.PUBLISHED, venue, 18);
        event.setEventDate(eventDate);
        Ticket t = new Ticket();
        t.setId(1L);
        t.setTicketCode("TCK-0001");
        t.setType(TicketType.GENERAL);
        t.setPrice(new java.math.BigDecimal("100000"));
        t.setStatus(status);
        t.setPurchaseDate(LocalDateTime.now());
        t.setUser(activeUser(LocalDate.of(2000, 1, 1)));
        t.setEvent(event);
        return t;
    }

    // TEST-TICKET-009
    @Test
    void cancel_paidTicket_changesToCancelled() {
        Ticket paid = ticket(TicketStatus.PAID, LocalDateTime.now().plusDays(10));
        when(ticketRepository.findByTicketCode("TCK-0001")).thenReturn(Optional.of(paid));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(mock(TicketResponse.class));

        ticketService.cancel("TCK-0001");

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TicketStatus.CANCELLED);
    }

    // TEST-TICKET-010
    @Test
    void cancel_usedTicket_throwsBusinessRuleAndNeverSaves() {
        Ticket used = ticket(TicketStatus.USED, LocalDateTime.now().plusDays(10));
        when(ticketRepository.findByTicketCode("TCK-0001")).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> ticketService.cancel("TCK-0001"))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void cancel_afterEventDate_throwsBusinessRule() {
        Ticket paidPastEvent = ticket(TicketStatus.PAID, LocalDateTime.now().minusDays(1));
        when(ticketRepository.findByTicketCode("TCK-0001")).thenReturn(Optional.of(paidPastEvent));

        assertThatThrownBy(() -> ticketService.cancel("TCK-0001"))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    // TEST-TICKET-011
    @Test
    void markAsUsed_paidTicket_changesToUsed() {
        Ticket paid = ticket(TicketStatus.PAID, LocalDateTime.now().plusDays(10));
        when(ticketRepository.findByTicketCode("TCK-0001")).thenReturn(Optional.of(paid));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(mock(TicketResponse.class));

        ticketService.markAsUsed("TCK-0001");

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TicketStatus.USED);
    }

    // TEST-TICKET-012
    @Test
    void markAsUsed_cancelledTicket_throwsBusinessRuleAndNeverSaves() {
        Ticket cancelled = ticket(TicketStatus.CANCELLED, LocalDateTime.now().plusDays(10));
        when(ticketRepository.findByTicketCode("TCK-0001")).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> ticketService.markAsUsed("TCK-0001"))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }
}