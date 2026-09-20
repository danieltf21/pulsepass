package com.pulsepass.repository;

import com.pulsepass.domain.Venue;
import org.hibernate.internal.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue, Long> {
    Optional<Venue> findByCode(String code);
}