package com.pulsepass.services  ;

import com.pulsepass.domain.Artist;
import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.ArtistMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.services.impl.ArtistServiceImpl;
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
class ArtistServiceImplTest {

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private ArtistMapper artistMapper;

    @InjectMocks
    private ArtistServiceImpl artistService;

    private Artist artist(Long id, String stageName, boolean active) {
        Artist a = new Artist();
        a.setId(id);
        a.setStageName(stageName);
        a.setCountry("Colombia");
        a.setGenre("Electronic");
        a.setActive(active);
        return a;
    }

    @Test
    void findById_existing_returnsDto() {
        Artist artist = artist(1L, "Solar Beat", true);
        ArtistResponse response = new ArtistResponse(1L, "Solar Beat", "Colombia", "Electronic", true);
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));
        when(artistMapper.toResponse(artist)).thenReturn(response);

        assertThat(artistService.findById(1L).stageName()).isEqualTo("Solar Beat");
    }

    @Test
    void findById_missing_throwsResourceNotFound() {
        when(artistRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artistService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByStageName_missing_throwsResourceNotFound() {
        when(artistRepository.findByStageNameIgnoreCase("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artistService.findByStageName("nadie"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findActiveArtists_returnsList() {
        Artist artist = artist(1L, "Solar Beat", true);
        ArtistResponse response = new ArtistResponse(1L, "Solar Beat", "Colombia", "Electronic", true);
        when(artistRepository.findByActiveTrueOrderByStageNameAsc()).thenReturn(List.of(artist));
        when(artistMapper.toResponse(artist)).thenReturn(response);

        List<ArtistResponse> result = artistService.findActiveArtists();

        assertThat(result).extracting(ArtistResponse::stageName).containsExactly("Solar Beat");
    }
}