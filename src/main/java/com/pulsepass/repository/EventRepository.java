package com.pulsepass.repository;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    // FR-EVT-001 / AC-002: Query Method
    Optional<Event> findByEventCode(String eventCode);

    // FR-EVT-005: Query Method
    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    // FR-VEN-004: Query Method navegando venue.code
    List<Event> findByVenue_Code(String venueCode);

    // FR-SRC-001 / FR-ART-004: JPQL con JOIN, DISTINCT evita duplicados
    @Query("""
           SELECT DISTINCT e FROM Event e
           JOIN e.artists a
           WHERE a.stageName = :stageName
           """)
    List<Event> findEventsByArtistStageName(@Param("stageName") String stageName);

    // FR-SRC-002: JPQL con varias asociaciones
    @Query("""
           SELECT DISTINCT e FROM Event e
           JOIN e.venue v
           JOIN e.artists a
           WHERE v.city = :city AND a.stageName = :stageName
           """)
    List<Event> findEventsByCityAndArtist(@Param("city") String city,
                                          @Param("stageName") String stageName);

    // FR-SRC-003: recomendados
    @Query("""
           SELECT DISTINCT e FROM Event e
           JOIN e.venue v
           JOIN e.artists a
           WHERE e.status = com.pulsepass.domain.enums.EventStatus.PUBLISHED
             AND e.eventDate > :date
             AND v.city = :city
             AND LOWER(a.stageName) LIKE LOWER(CONCAT('%', :artistText, '%'))
           ORDER BY e.eventDate ASC
           """)
    List<Event> findRecommendedEvents(@Param("date") LocalDateTime date,
                                      @Param("city") String city,
                                      @Param("artistText") String artistText);
}