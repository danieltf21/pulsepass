package com.pulsepass.services;

import com.pulsepass.domain.*;
import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.domain.enums.TicketStatus;
import com.pulsepass.domain.enums.TicketType;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.mapper.ArtistMapper;
import com.pulsepass.mapper.EventMapper;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.services.impl.EventServiceImpl;
import com.pulsepass.services.impl.TicketServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PulsePassAcceptanceScenarioTest {

    @Mock private VenueRepository venueRepository;
    @Mock private EventRepository eventRepository;
    @Mock private ArtistRepository artistRepository;
    @Mock private UserRepository userRepository;
    @Mock private TicketRepository ticketRepository;

    private EventServiceImpl eventService;
    private TicketServiceImpl ticketService;

    private final Map<String, Event> eventsByCode = new HashMap<>();
    private final Map<Long, Artist> artistsById = new HashMap<>();
    private final Map<String, Ticket> ticketsByCode = new HashMap<>();
    private long ticketSequence = 1L;

    private Venue venue;
    private User andrea;
    private User carlos;
    private User laura;
    private User miguel;
    private Artist solarBeat;
    private Artist neonWaves;
    private Artist caribbeanSound;

    @BeforeEach
    void setUp() {
        // Mappers "identidad": no interesa el DTO exacto para este escenario,
        // solo que el flujo de reglas de negocio se ejecute correctamente.
        EventMapper eventMapper = buildSimpleEventMapper();
        ArtistMapper artistMapper = event -> null; // no usado directamente aquí
        TicketMapper ticketMapper = buildSimpleTicketMapper();

        eventService = new EventServiceImpl(eventRepository, venueRepository, artistRepository, eventMapper);
        ticketService = new TicketServiceImpl(userRepository, eventRepository, ticketRepository, ticketMapper);

        // Venue: capacidad 3 (PRD sección 47)
        venue = new Venue();
        venue.setId(1L);
        venue.setCode("VEN-SMR-01");
        venue.setName("Marina Convention Center");
        venue.setCity("Santa Marta");
        venue.setAddress("Calle 1");
        venue.setCapacity(3);
        venue.setActive(true);

        // Artistas
        solarBeat = artist(1L, "Solar Beat");
        neonWaves = artist(2L, "Neon Waves");
        caribbeanSound = artist(3L, "Caribbean Sound");
        artistsById.put(1L, solarBeat);
        artistsById.put(2L, neonWaves);
        artistsById.put(3L, caribbeanSound);

        // Usuarios (edades calculadas respecto a la fecha del evento: hoy + 2 meses)
        LocalDateTime eventDate = LocalDateTime.now().plusMonths(2);
        andrea = user(1L, "andrea", "andrea@email.com", true, eventDate.toLocalDate().minusYears(25));
        carlos = user(2L, "carlos", "carlos@email.com", true, eventDate.toLocalDate().minusYears(21));
        laura = user(3L, "laura", "laura@email.com", true, eventDate.toLocalDate().minusYears(17));
        miguel = user(4L, "miguel", "miguel@email.com", false, eventDate.toLocalDate().minusYears(30));

        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(eventRepository.existsByEventCode(anyString()))
                .thenAnswer(inv -> eventsByCode.containsKey(inv.getArgument(0, String.class)));
        when(eventRepository.findByEventCode(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(eventsByCode.get(inv.getArgument(0, String.class))));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            eventsByCode.put(e.getEventCode(), e);
            return e;
        });
        when(artistRepository.findById(any(Long.class)))
                .thenAnswer(inv -> Optional.ofNullable(artistsById.get(inv.getArgument(0, Long.class))));

        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(andrea));
        when(userRepository.findByEmailIgnoreCase("carlos@email.com")).thenReturn(Optional.of(carlos));
        when(userRepository.findByEmailIgnoreCase("laura@email.com")).thenReturn(Optional.of(laura));
        when(userRepository.findByEmailIgnoreCase("miguel@email.com")).thenReturn(Optional.of(miguel));

        when(ticketRepository.countByEventEventCodeAndStatus(anyString(), any()))
                .thenAnswer(inv -> ticketsByCode.values().stream()
                        .filter(t -> t.getEvent().getEventCode().equals(inv.getArgument(0, String.class)))
                        .filter(t -> t.getStatus() == inv.getArgument(1, TicketStatus.class))
                        .count());
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            if (t.getTicketCode() == null) {
                t.setTicketCode("TCK-" + ticketSequence++);
            }
            ticketsByCode.put(t.getTicketCode(), t);
            return t;
        });
        when(ticketRepository.findByTicketCode(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(ticketsByCode.get(inv.getArgument(0, String.class))));
    }

    private Artist artist(Long id, String stageName) {
        Artist a = new Artist();
        a.setId(id);
        a.setStageName(stageName);
        a.setActive(true);
        return a;
    }

    private User user(Long id, String username, String email, boolean active, LocalDate birthDate) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(email);
        u.setActive(active);
        UserProfile profile = new UserProfile();
        profile.setFirstName(username);
        profile.setLastName("Test");
        profile.setBirthDate(birthDate);
        profile.setUser(u);
        u.setProfile(profile);
        return u;
    }

    private EventMapper buildSimpleEventMapper() {
        return new EventMapper() {
            @Override
            public EventResponse toResponse(Event event) {
                return new EventResponse(event.getId(), event.getEventCode(), event.getName(),
                        event.getDescription(), event.getCategory(), event.getStatus(), event.getEventDate(),
                        event.getMinimumAge(), event.getVenue().getCode(), event.getVenue().getName(), java.util.Set.of());
            }

            @Override
            public com.pulsepass.dto.response.EventSummaryResponse toSummary(Event event) {
                return new com.pulsepass.dto.response.EventSummaryResponse(event.getEventCode(), event.getName(),
                        event.getStatus(), event.getEventDate(), event.getVenue().getName());
            }
        };
    }

    private TicketMapper buildSimpleTicketMapper() {
        return ticket -> new TicketResponse(ticket.getId(), ticket.getTicketCode(), ticket.getType(),
                ticket.getPrice(), ticket.getStatus(), ticket.getPurchaseDate(),
                ticket.getUser().getEmail(), ticket.getEvent().getEventCode(), ticket.getEvent().getName());
    }

    @Test
    void fullAcceptanceScenario() {
        LocalDateTime eventDate = LocalDateTime.now().plusMonths(2);
        CreateEventRequest createRequest = new CreateEventRequest(
                "CMF-2026", "Caribbean Music Fest 2026", "Festival de música caribeña",
                EventCategory.MUSIC, eventDate, 18, "VEN-SMR-01");

        // AC-001: CMF-2026 puede crearse y debe iniciar DRAFT
        EventResponse created = eventService.create(createRequest);
        assertThat(created.status()).isEqualTo(EventStatus.DRAFT);
        assertThat(eventsByCode.get("CMF-2026").getArtists()).isNotNull();
        eventsByCode.get("CMF-2026").setArtists(new HashSet<>());

        // AC-003: se pueden asociar los tres artistas
        eventService.addArtist("CMF-2026", 1L);
        eventService.addArtist("CMF-2026", 2L);
        eventService.addArtist("CMF-2026", 3L);
        assertThat(eventsByCode.get("CMF-2026").getArtists())
                .containsExactlyInAnyOrder(solarBeat, neonWaves, caribbeanSound);

        // AC-002: puede publicarse cuando las reglas se cumplen
        EventResponse published = eventService.publish("CMF-2026");
        assertThat(published.status()).isEqualTo(EventStatus.PUBLISHED);

        // AC-004: Andrea (25 años, activa) puede comprar
        TicketResponse andreaTicket = ticketService.purchase(
                new PurchaseTicketRequest("andrea@email.com", "CMF-2026", TicketType.VIP));
        assertThat(andreaTicket.status()).isEqualTo(TicketStatus.PAID);

        // AC-005: Carlos (21 años, activo) puede comprar
        TicketResponse carlosTicket = ticketService.purchase(
                new PurchaseTicketRequest("carlos@email.com", "CMF-2026", TicketType.GENERAL));
        assertThat(carlosTicket.status()).isEqualTo(TicketStatus.PAID);

        // AC-006: Laura (17 años) no puede comprar por edad mínima
        assertThatThrownBy(() -> ticketService.purchase(
                new PurchaseTicketRequest("laura@email.com", "CMF-2026", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);

        // AC-007: Miguel (inactivo) no puede comprar
        assertThatThrownBy(() -> ticketService.purchase(
                new PurchaseTicketRequest("miguel@email.com", "CMF-2026", TicketType.VIP)))
                .isInstanceOf(BusinessRuleException.class);

        // AC-008: un tercer usuario adulto puede adquirir el último ticket (capacidad 3) y el evento pasa a SOLD_OUT
        User thirdUser = user(5L, "nicolas", "nicolas@email.com", true, eventDate.toLocalDate().minusYears(30));
        when(userRepository.findByEmailIgnoreCase("nicolas@email.com")).thenReturn(Optional.of(thirdUser));
        TicketResponse thirdTicket = ticketService.purchase(
                new PurchaseTicketRequest("nicolas@email.com", "CMF-2026", TicketType.STUDENT));
        assertThat(thirdTicket.status()).isEqualTo(TicketStatus.PAID);
        assertThat(eventsByCode.get("CMF-2026").getStatus()).isEqualTo(EventStatus.SOLD_OUT);

        // AC-009: una cuarta compra debe producir BusinessRuleException
        User fourthUser = user(6L, "valeria", "valeria@email.com", true, eventDate.toLocalDate().minusYears(28));
        when(userRepository.findByEmailIgnoreCase("valeria@email.com")).thenReturn(Optional.of(fourthUser));
        assertThatThrownBy(() -> ticketService.purchase(
                new PurchaseTicketRequest("valeria@email.com", "CMF-2026", TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);

        // AC-010: un ticket PAID puede pasar a USED
        TicketResponse usedTicket = ticketService.markAsUsed(andreaTicket.ticketCode());
        assertThat(usedTicket.status()).isEqualTo(TicketStatus.USED);

        // AC-011: un ticket USED no puede cancelarse
        assertThatThrownBy(() -> ticketService.cancel(andreaTicket.ticketCode()))
                .isInstanceOf(BusinessRuleException.class);
    }
}
