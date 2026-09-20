package com.pulsepass.repository;

import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // FR-TKT-006
    List<Ticket> findByUser_Email(String email);
    List<Ticket> findByUser_EmailAndStatus(String email, TicketStatus status);

    // FR-TKT-007: Query Method (justificación: filtro simple por dos campos, sin joins explícitos)
    List<Ticket> findByEvent_EventCodeAndStatus(String eventCode, TicketStatus status);

    // FR-TKT-008: JPQL con COUNT
    @Query("""
           SELECT COUNT(t) FROM Ticket t
           WHERE t.event.eventCode = :eventCode
             AND t.status = com.pulsepass.domain.enums.TicketStatus.PAID
           """)
    long countPaidByEventCode(@Param("eventCode") String eventCode);

    // FR-SRC-004
    @Query("""
           SELECT t FROM Ticket t
           JOIN t.event e
           WHERE e.eventDate > :date
           ORDER BY e.eventDate ASC
           """)
    List<Ticket> findTicketsOfEventsAfter(@Param("date") LocalDateTime date);
}
