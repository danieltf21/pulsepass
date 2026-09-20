package com.pulsepass;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
class EventArtistIT {

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @PersistenceContext
    private EntityManager em;

    private Venue venue;
    private Artist solarBeat;
    private Artist neonWaves;
    private Artist caribbeanSound;

    @BeforeEach
    void setUp() {
        venue = venueRepository.save(TestData.venue("VEN-SMR-01", "Santa Marta"));
        solarBeat = artistRepository.findByStageName("Solar Beat").orElseThrow();
        neonWaves = artistRepository.findByStageName("Neon Waves").orElseThrow();
        caribbeanSound = artistRepository.findByStageName("Caribbean Sound").orElseThrow();
    }

    @Test
    void initialArtistsExistFromMigrationV2() {
        assertThat(artistRepository.findAll())
                .extracting(Artist::getStageName)
                .contains("Solar Beat", "Neon Waves", "Caribbean Sound", "Ocean Drive", "Digital Pulse");
    }

    @Test
    void eventWithThreeArtistsIsPersisted() {
        Event event = TestData.event("CMF-2026", venue, EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 1, 20, 0));
        event.getArtists().add(solarBeat);
        event.getArtists().add(neonWaves);
        event.getArtists().add(caribbeanSound);
        Long id = eventRepository.save(event).getId();

        em.flush();
        em.clear();

        Event reloaded = eventRepository.findById(id).orElseThrow();
        assertThat(reloaded.getArtists()).hasSize(3);
    }

    @Test
    void sameArtistAddedTwiceIsStoredOnce() {
        Event event = TestData.event("EV-DUP-ART", venue, EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 1, 20, 0));
        event.getArtists().add(solarBeat);
        event.getArtists().add(solarBeat);
        Long id = eventRepository.save(event).getId();

        em.flush();
        em.clear();

        assertThat(eventRepository.findById(id).orElseThrow().getArtists()).hasSize(1);
    }

    @Test
    void artistEventsAppearOnlyOnce() {
        Event e1 = TestData.event("EV-1", venue, EventStatus.PUBLISHED, LocalDateTime.of(2026, 12, 1, 20, 0));
        e1.getArtists().add(solarBeat);
        e1.getArtists().add(neonWaves);
        Event e2 = TestData.event("EV-2", venue, EventStatus.PUBLISHED, LocalDateTime.of(2026, 12, 2, 20, 0));
        e2.getArtists().add(solarBeat);
        Event e3 = TestData.event("EV-3", venue, EventStatus.PUBLISHED, LocalDateTime.of(2026, 12, 3, 20, 0));
        e3.getArtists().add(neonWaves);
        eventRepository.save(e1);
        eventRepository.save(e2);
        eventRepository.save(e3);

        assertThat(eventRepository.findEventsByArtistStageName("Solar Beat"))
                .extracting(Event::getEventCode)
                .containsExactlyInAnyOrder("EV-1", "EV-2");
        assertThat(eventRepository.findEventsByArtistStageName("Neon Waves"))
                .extracting(Event::getEventCode)
                .containsExactlyInAnyOrder("EV-1", "EV-3");
    }
}