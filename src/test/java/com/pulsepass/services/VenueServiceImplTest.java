package com.pulsepass.services;

import com.pulsepass.domain.Venue;
import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.VenueMapper;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.services.impl.VenueServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueServiceImplTest {

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private VenueMapper venueMapper;

    @InjectMocks
    private VenueServiceImpl venueService;

    private Venue venue(String code, boolean active) {
        Venue v = new Venue();
        v.setId(1L);
        v.setCode(code);
        v.setName("Marina Convention Center");
        v.setCity("Santa Marta");
        v.setAddress("Calle 1 # 2-3");
        v.setCapacity(3);
        v.setActive(active);
        return v;
    }

    @Test
    void findByCode_existingVenue_returnsDto() {
        Venue venue = venue("VEN-SMR-01", true);
        VenueResponse response = new VenueResponse(1L, "VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Calle 1 # 2-3", 3, true);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(venueMapper.toResponse(venue)).thenReturn(response);

        VenueResponse result = venueService.findByCode("VEN-SMR-01");

        assertThat(result.code()).isEqualTo("VEN-SMR-01");
    }

    @Test
    void findByCode_missingVenue_throwsResourceNotFound() {
        when(venueRepository.findByCode("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> venueService.findByCode("NOPE"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findActiveVenues_returnsOnlyActiveOnes() {
        Venue active = venue("VEN-A", true);
        VenueResponse response = new VenueResponse(1L, "VEN-A", "Marina", "Santa Marta", "Calle 1", 3, true);
        when(venueRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(active));
        when(venueMapper.toResponse(active)).thenReturn(response);

        List<VenueResponse> result = venueService.findActiveVenues();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).active()).isTrue();
    }
}