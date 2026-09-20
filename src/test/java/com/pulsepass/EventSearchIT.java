package com.pulsepass;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.domain.enums.TicketType;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class EventSearchIT {

    private static final LocalDateTime CUTOFF = LocalDateTime.of(2026, 10, 1, 0, 0);

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    private Venue santaMarta;
    private Venue bogota;
    private Artist solarBeat;
    private Artist neonWaves;

    private Event saveEvent(String code, Venue venue, EventStatus status,
                            LocalDateTime date, Artist... artists) {
        Event event = TestData.event(code, venue, status, date);
        for (Artist artist : artists) {
            event.getArtists().add(artist);
        }
        return eventRepository.save(event);
    }

    @BeforeEach
    void setUp() {
        santaMarta = venueRepository.save(TestData.venue("VEN-SMR-01", "Santa Marta"));
        bogota = venueRepository.save(TestData.venue("VEN-BOG-01", "Bogotá"));
        solarBeat = artistRepository.findByStageName("Solar Beat").orElseThrow();
        neonWaves = artistRepository.findByStageName("Neon Waves").orElseThrow();

        saveEvent("EV-1", santaMarta, EventStatus.PUBLISHED, LocalDateTime.of(2026, 12, 10, 20, 0), solarBeat, neonWaves);
        saveEvent("EV-2", santaMarta, EventStatus.PUBLISHED, LocalDateTime.of(2026, 11, 5, 20, 0), solarBeat);
        saveEvent("EV-3", bogota, EventStatus.PUBLISHED, LocalDateTime.of(2026, 11, 20, 20, 0), solarBeat);
        saveEvent("EV-4", santaMarta, EventStatus.DRAFT, LocalDateTime.of(2026, 11, 15, 20, 0), solarBeat);
        saveEvent("EV-5", santaMarta, EventStatus.PUBLISHED, LocalDateTime.of(2026, 8, 1, 20, 0), solarBeat);
        saveEvent("EV-6", santaMarta, EventStatus.PUBLISHED, LocalDateTime.of(2026, 11, 25, 20, 0), neonWaves);
    }

    @Test
    void findsEventsByCityAndArtist() {
        assertThat(eventRepository.findEventsByCityAndArtist("Santa Marta", "Solar Beat"))
                .extracting(Event::getEventCode)
                .containsExactlyInAnyOrder("EV-1", "EV-2", "EV-4", "EV-5");
    }

    @Test
    void recommendedEventsFilterByStatusDateCityAndArtistAndAreOrdered() {
        assertThat(eventRepository.findRecommendedEvents(CUTOFF, "Santa Marta", "SOLAR"))
                .extracting(Event::getEventCode)
                .containsExactly("EV-2", "EV-1");
    }

    @Test
    void recommendedEventsDoNotRepeatEvents() {
        assertThat(eventRepository.findRecommendedEvents(CUTOFF, "Santa Marta", "a"))
                .extracting(Event::getEventCode)
                .containsExactly("EV-2", "EV-6", "EV-1");
    }

    @Test
    void ticketsOfFutureEventsAreOrderedByEventDate() {
        User user = userRepository.save(TestData.user("andrea", "andrea@mail.com"));
        Event past = eventRepository.findByEventCode("EV-5").orElseThrow();
        Event soon = eventRepository.findByEventCode("EV-2").orElseThrow();
        Event later = eventRepository.findByEventCode("EV-1").orElseThrow();
        ticketRepository.save(TestData.ticket("TCK-PAST", user, past, TicketType.GENERAL, TicketStatus.PAID, "100000"));
        ticketRepository.save(TestData.ticket("TCK-LATER", user, later, TicketType.VIP, TicketStatus.PAID, "250000"));
        ticketRepository.save(TestData.ticket("TCK-SOON", user, soon, TicketType.GENERAL, TicketStatus.PAID, "100000"));

        assertThat(ticketRepository.findTicketsOfEventsAfter(CUTOFF))
                .extracting(Ticket::getTicketCode)
                .containsExactly("TCK-SOON", "TCK-LATER");
    }
}