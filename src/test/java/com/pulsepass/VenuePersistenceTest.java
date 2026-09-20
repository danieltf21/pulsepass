package com.pulsepass;

import com.pulsepass.domain.Venue;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class VenuePersistenceTest {

    @Autowired
    private VenueRepository venueRepository;

    @Test
    void persistsAndFindsByIdAndCode() {
        Venue saved = venueRepository.saveAndFlush(TestData.venue("VEN-SMR-01", "Santa Marta"));

        assertThat(saved.getId()).isNotNull();
        assertThat(venueRepository.findById(saved.getId())).isPresent();
        Venue byCode = venueRepository.findByCode("VEN-SMR-01").orElseThrow();
        assertThat(byCode.getCapacity()).isGreaterThan(0);
        assertThat(byCode.getCity()).isEqualTo("Santa Marta");
    }

    @Test
    void rejectsDuplicateCode() {
        venueRepository.saveAndFlush(TestData.venue("VEN-DUP", "Santa Marta"));

        assertThrows(DataIntegrityViolationException.class,
                () -> venueRepository.saveAndFlush(TestData.venue("VEN-DUP", "Bogotá")));
    }

    @Test
    void rejectsNonPositiveCapacity() {
        Venue venue = TestData.venue("VEN-CAP", "Santa Marta");
        venue.setCapacity(0);

        assertThrows(DataIntegrityViolationException.class,
                () -> venueRepository.saveAndFlush(venue));
    }
}