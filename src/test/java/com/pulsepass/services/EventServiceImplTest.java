package com.pulsepass.services;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.EventMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.services.impl.EventServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock private EventRepository eventRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private ArtistRepository artistRepository;
    @Mock private EventMapper eventMapper;

    @InjectMocks
    private EventServiceImpl eventService;

    private Venue venue(boolean active) {
        Venue v = new Venue();
        v.setId(1L);
        v.setCode("VEN-SMR-01");
        v.setName("Marina Convention Center");
        v.setCity("Santa Marta");
        v.setAddress("Calle 1");
        v.setCapacity(3);
        v.setActive(active);
        return v;
    }

    private Event event(EventStatus status, LocalDateTime date, Venue venue) {
        Event e = new Event();
        e.setId(1L);
        e.setEventCode("CMF-2026");
        e.setName("Caribbean Music Fest 2026");
        e.setCategory(EventCategory.MUSIC);
        e.setStatus(status);
        e.setEventDate(date);
        e.setMinimumAge(18);
        e.setVenue(venue);
        return e;
    }

    private CreateEventRequest validRequest() {
        return new CreateEventRequest("CMF-2026", "Caribbean Music Fest 2026", "desc",
                EventCategory.MUSIC, LocalDateTime.now().plusMonths(2), 18, "VEN-SMR-01");
    }

    // TEST-EVENT-001
    @Test
    void findByCode_existingEvent_returnsDto() {
        Venue venue = venue(true);
        Event event = event(EventStatus.PUBLISHED, LocalDateTime.now().plusDays(10), venue);
        EventResponse response = mock(EventResponse.class);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(eventMapper.toResponse(event)).thenReturn(response);

        assertThat(eventService.findByCode("CMF-2026")).isEqualTo(response);
    }

    // TEST-EVENT-002
    @Test
    void findByCode_missingEvent_throwsResourceNotFound() {
        when(eventRepository.findByEventCode("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findByCode("NOPE"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // TEST-EVENT-003
    @Test
    void create_validRequest_savesEventInDraft() {
        Venue venue = venue(true);
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventMapper.toResponse(any(Event.class))).thenReturn(mock(EventResponse.class));

        eventService.create(validRequest());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(captor.getValue().getEventCode()).isEqualTo("CMF-2026");
    }

    @Test
    void create_duplicateEventCode_throwsDuplicateResource() {
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(true);

        assertThatThrownBy(() -> eventService.create(validRequest()))
                .isInstanceOf(DuplicateResourceException.class);

        verify(eventRepository, never()).save(any());
    }

    // TEST-EVENT-004
    @Test
    void create_venueDoesNotExist_throwsResourceNotFoundAndNeverSaves() {
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.create(validRequest()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(eventRepository, never()).save(any());
    }

    // TEST-EVENT-005
    @Test
    void create_inactiveVenue_throwsBusinessRuleAndNeverSaves() {
        Venue inactiveVenue = venue(false);
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(inactiveVenue));

        assertThatThrownBy(() -> eventService.create(validRequest()))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }

    // TEST-EVENT-006
    @Test
    void create_pastDate_throwsBusinessRuleAndNeverSaves() {
        Venue venue = venue(true);
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        CreateEventRequest pastRequest = new CreateEventRequest("CMF-2026", "Fest", "desc",
                EventCategory.MUSIC, LocalDateTime.now().minusDays(1), 18, "VEN-SMR-01");

        assertThatThrownBy(() -> eventService.create(pastRequest))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }

    // TEST-EVENT-007
    @Test
    void publish_validDraft_changesToPublished() {
        Venue venue = venue(true);
        Event draft = event(EventStatus.DRAFT, LocalDateTime.now().plusDays(10), venue);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(draft));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventMapper.toResponse(any(Event.class))).thenReturn(mock(EventResponse.class));

        eventService.publish("CMF-2026");

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }

    // TEST-EVENT-008
    @Test
    void publish_cancelledEvent_throwsBusinessRuleAndNeverSaves() {
        Venue venue = venue(true);
        Event cancelled = event(EventStatus.CANCELLED, LocalDateTime.now().plusDays(10), venue);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> eventService.publish("CMF-2026"))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }

    // BR-EVENT-010 / 011 (FR-SVC-007, "unit test requerido")
    @Test
    void addArtist_duplicateArtist_throwsBusinessRule() {
        Venue venue = venue(true);
        Event published = event(EventStatus.PUBLISHED, LocalDateTime.now().plusDays(10), venue);
        Artist solarBeat = new Artist();
        solarBeat.setId(1L);
        solarBeat.setStageName("Solar Beat");
        published.setArtists(new java.util.HashSet<>(Set.of(solarBeat)));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(published));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(solarBeat));

        assertThatThrownBy(() -> eventService.addArtist("CMF-2026", 1L))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void addArtist_cancelledEvent_throwsBusinessRule() {
        Venue venue = venue(true);
        Event cancelled = event(EventStatus.CANCELLED, LocalDateTime.now().plusDays(10), venue);
        Artist solarBeat = new Artist();
        solarBeat.setId(1L);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(cancelled));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(solarBeat));

        assertThatThrownBy(() -> eventService.addArtist("CMF-2026", 1L))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }
}