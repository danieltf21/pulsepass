package com.pulsepass;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.domain.enums.TicketType;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class TicketRepositoryIT {

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    private Event event;
    private User andrea;
    private User carlos;
    private User laura;
    private User miguel;

    @BeforeEach
    void setUp() {
        Venue venue = venueRepository.save(TestData.venue("VEN-SMR-01", "Santa Marta"));
        event = eventRepository.save(TestData.event("CMF-2026", venue, EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 1, 20, 0)));

        andrea = userRepository.save(TestData.user("andrea", "andrea@mail.com"));
        carlos = userRepository.save(TestData.user("carlos", "carlos@mail.com"));
        laura = userRepository.save(TestData.user("laura", "laura@mail.com"));
        miguel = userRepository.save(TestData.user("miguel", "miguel@mail.com"));

        ticketRepository.save(TestData.ticket("TCK-0001", andrea, event, TicketType.VIP, TicketStatus.PAID, "250000"));
        ticketRepository.save(TestData.ticket("TCK-0002", carlos, event, TicketType.GENERAL, TicketStatus.PAID, "120000"));
        ticketRepository.save(TestData.ticket("TCK-0003", laura, event, TicketType.GENERAL, TicketStatus.RESERVED, "120000"));
        ticketRepository.save(TestData.ticket("TCK-0004", miguel, event, TicketType.VIP, TicketStatus.CANCELLED, "250000"));
    }

    @Test
    void countsOnlyPaidTicketsOfEvent() {
        assertThat(ticketRepository.countPaidByEventCode("CMF-2026")).isEqualTo(2);
    }

    @Test
    void findsPaidTicketsByEventCode() {
        List<Ticket> paid = ticketRepository.findByEvent_EventCodeAndStatus("CMF-2026", TicketStatus.PAID);

        assertThat(paid).extracting(Ticket::getTicketCode)
                .containsExactlyInAnyOrder("TCK-0001", "TCK-0002");
    }

    @Test
    void findsTicketsByUserEmail() {
        List<Ticket> tickets = ticketRepository.findByUser_Email("andrea@mail.com");

        assertThat(tickets).hasSize(1);
        Ticket ticket = tickets.get(0);
        assertThat(ticket.getType()).isEqualTo(TicketType.VIP);
        assertThat(ticket.getPrice()).isEqualByComparingTo("250000");
        assertThat(ticket.getEvent().getEventCode()).isEqualTo("CMF-2026");
    }

    @Test
    void findsTicketsByUserEmailAndStatus() {
        assertThat(ticketRepository.findByUser_EmailAndStatus("laura@mail.com", TicketStatus.RESERVED))
                .hasSize(1);
        assertThat(ticketRepository.findByUser_EmailAndStatus("andrea@mail.com", TicketStatus.CANCELLED))
                .isEmpty();
    }

    @Test
    void rejectsDuplicateTicketCode() {
        Ticket duplicate = TestData.ticket("TCK-0001", laura, event, TicketType.STUDENT, TicketStatus.RESERVED, "50000");

        assertThrows(DataIntegrityViolationException.class,
                () -> ticketRepository.saveAndFlush(duplicate));
    }

    @Test
    void rejectsNegativePrice() {
        Ticket negative = TestData.ticket("TCK-NEG", laura, event, TicketType.GENERAL, TicketStatus.RESERVED, "-1");

        assertThrows(DataIntegrityViolationException.class,
                () -> ticketRepository.saveAndFlush(negative));
    }

    @Test
    void rejectsTicketWithoutUser() {
        Ticket orphan = TestData.ticket("TCK-ORF", null, event, TicketType.GENERAL, TicketStatus.RESERVED, "100000");

        assertThrows(Exception.class, () -> ticketRepository.saveAndFlush(orphan));
    }
}