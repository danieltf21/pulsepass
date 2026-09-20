package com.pulsepass;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
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
class EventRepositoryIT {

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void findsEventByCodeWithVenue() {
        Venue venue = venueRepository.save(TestData.venue("VEN-SMR-01", "Santa Marta"));
        eventRepository.save(TestData.event("CMF-2026", venue, EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 1, 20, 0)));

        Event found = eventRepository.findByEventCode("CMF-2026").orElseThrow();

        assertThat(found.getVenue().getCode()).isEqualTo("VEN-SMR-01");
    }

    @Test
    void publishedEventsAreOrderedByDateAscending() {
        Venue venue = venueRepository.save(TestData.venue("VEN-SMR-01", "Santa Marta"));
        eventRepository.save(TestData.event("EV-3", venue, EventStatus.PUBLISHED, LocalDateTime.of(2026, 12, 30, 20, 0)));
        eventRepository.save(TestData.event("EV-1", venue, EventStatus.PUBLISHED, LocalDateTime.of(2026, 11, 1, 20, 0)));
        eventRepository.save(TestData.event("EV-2", venue, EventStatus.PUBLISHED, LocalDateTime.of(2026, 12, 1, 20, 0)));
        eventRepository.save(TestData.event("EV-DRAFT", venue, EventStatus.DRAFT, LocalDateTime.of(2026, 11, 15, 20, 0)));
        eventRepository.save(TestData.event("EV-CANC", venue, EventStatus.CANCELLED, LocalDateTime.of(2026, 11, 20, 20, 0)));

        List<Event> result = eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED);

        assertThat(result).extracting(Event::getEventCode)
                .containsExactly("EV-1", "EV-2", "EV-3");
    }

    @Test
    void findsEventsOnlyOfRequestedVenue() {
        Venue venueA = venueRepository.save(TestData.venue("VEN-A", "Santa Marta"));
        Venue venueB = venueRepository.save(TestData.venue("VEN-B", "Bogotá"));
        eventRepository.save(TestData.event("EV-A", venueA, EventStatus.PUBLISHED, LocalDateTime.of(2026, 12, 1, 20, 0)));
        eventRepository.save(TestData.event("EV-B", venueB, EventStatus.PUBLISHED, LocalDateTime.of(2026, 12, 2, 20, 0)));

        List<Event> result = eventRepository.findByVenue_Code("VEN-A");

        assertThat(result).extracting(Event::getEventCode).containsExactly("EV-A");
    }

    @Test
    void rejectsDuplicateEventCode() {
        Venue venue = venueRepository.save(TestData.venue("VEN-SMR-01", "Santa Marta"));
        eventRepository.saveAndFlush(TestData.event("EV-DUP", venue, EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 1, 20, 0)));

        assertThrows(DataIntegrityViolationException.class,
                () -> eventRepository.saveAndFlush(TestData.event("EV-DUP", venue, EventStatus.DRAFT,
                        LocalDateTime.of(2026, 12, 2, 20, 0))));
    }

    @Test
    void rejectsNegativeMinimumAge() {
        Venue venue = venueRepository.save(TestData.venue("VEN-SMR-01", "Santa Marta"));
        Event event = TestData.event("EV-AGE", venue, EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 1, 20, 0));
        event.setMinimumAge(-1);

        assertThrows(DataIntegrityViolationException.class,
                () -> eventRepository.saveAndFlush(event));
    }

    @Test
    void streamingUrlIsOptional() {
        Venue venue = venueRepository.save(TestData.venue("VEN-SMR-01", "Santa Marta"));
        Event event = eventRepository.saveAndFlush(TestData.event("EV-STR", venue, EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 1, 20, 0)));
        assertThat(event.getStreamingUrl()).isNull();

        event.setStreamingUrl("https://stream.pulsepass.com/ev-str");
        Event updated = eventRepository.saveAndFlush(event);

        assertThat(updated.getStreamingUrl()).isEqualTo("https://stream.pulsepass.com/ev-str");
    }
}