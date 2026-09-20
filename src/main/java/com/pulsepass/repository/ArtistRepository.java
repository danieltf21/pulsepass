package com.pulsepass.repository;

import com.pulsepass.domain.Artist;
import org.hibernate.internal.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist, Long> {
    Optional<Artist> findByStageName(String stageName);
}