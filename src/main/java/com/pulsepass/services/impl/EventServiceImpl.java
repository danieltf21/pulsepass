package com.pulsepass.services.impl;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.Venue;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.EventMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.services.EventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final ArtistRepository artistRepository;
    private final EventMapper eventMapper;

    public EventServiceImpl(EventRepository eventRepository,
                            VenueRepository venueRepository,
                            ArtistRepository artistRepository,
                            EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.artistRepository = artistRepository;
        this.eventMapper = eventMapper;
    }

    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request) {
        // BR-EVENT-001
        if (eventRepository.existsByEventCode(request.eventCode())) {
            throw new DuplicateResourceException("Event code already exists: " + request.eventCode());
        }

        // BR-EVENT-002
        Venue venue = venueRepository.findByCode(request.venueCode())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + request.venueCode()));

        // BR-EVENT-003
        if (!venue.isActive()) {
            throw new BusinessRuleException("Cannot create an event in an inactive venue: " + request.venueCode());
        }

        // BR-EVENT-004
        if (request.eventDate() == null || !request.eventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Event date must be in the future");
        }

        // BR-EVENT-006
        if (request.minimumAge() == null || request.minimumAge() < 0) {
            throw new BusinessRuleException("Minimum age cannot be negative");
        }

        Event event = new Event();
        event.setEventCode(request.eventCode());
        event.setName(request.name());
        event.setDescription(request.description());
        event.setCategory(request.category());
        event.setEventDate(request.eventDate());
        event.setMinimumAge(request.minimumAge());
        event.setVenue(venue);
        event.setStatus(EventStatus.DRAFT); // BR-EVENT-005

        Event saved = eventRepository.save(event);
        return eventMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse findByCode(String eventCode) {
        return eventRepository.findByEventCode(eventCode)
                .map(eventMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findPublishedEvents() {
        return eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED)
                .stream()
                .map(eventMapper::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public EventResponse publish(String eventCode) {
        Event event = eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventCode));

        // BR-EVENT-007
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException("Only a DRAFT event can be published: " + eventCode);
        }

        // BR-EVENT-008
        if (!event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot publish an event with a past date: " + eventCode);
        }

        // BR-EVENT-009
        if (!event.getVenue().isActive()) {
            throw new BusinessRuleException("Cannot publish an event whose venue is inactive: " + eventCode);
        }

        event.setStatus(EventStatus.PUBLISHED);
        Event saved = eventRepository.save(event);
        return eventMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public EventResponse addArtist(String eventCode, Long artistId) {
        Event event = eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventCode));

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found: " + artistId));

        // BR-EVENT-011
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.FINISHED) {
            throw new BusinessRuleException("Cannot add artists to an event in status " + event.getStatus());
        }

        // BR-EVENT-010
        if (event.getArtists().contains(artist)) {
            throw new BusinessRuleException("Artist already associated with event: " + eventCode);
        }

        event.getArtists().add(artist);
        Event saved = eventRepository.save(event);
        return eventMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findByArtist(String stageName) {
        return eventRepository.findEventsByArtistStageName(stageName)
                .stream()
                .map(eventMapper::toSummary)
                .toList();
    }
}
