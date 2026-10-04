package com.pulsepass.services;

import com.pulsepass.domain.User;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.mapper.UserMapper;
import com.pulsepass.repository.UserProfileRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.services.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private RegisterUserRequest validRequest() {
        return new RegisterUserRequest("andrea", "andrea@email.com", "Andrea", "Gómez",
                "3001234567", "Santa Marta", LocalDate.of(2000, 5, 10));
    }

    // TEST-USER-001
    @Test
    void register_validRequest_savesUserAndProfile() {
        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(userMapper.toResponse(any(User.class))).thenReturn(mock(com.pulsepass.dto.response.UserResponse.class));

        userService.register(validRequest());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().isActive()).isTrue();
        verify(userProfileRepository).save(any());
    }

    // TEST-USER-002
    @Test
    void register_duplicateUsername_throwsDuplicateResourceAndNeverSaves() {
        when(userRepository.existsByUsername("andrea")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(validRequest()))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
        verify(userProfileRepository, never()).save(any());
    }

    // TEST-USER-003
    @Test
    void register_duplicateEmail_throwsDuplicateResourceAndNeverSaves() {
        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(validRequest()))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    // TEST-USER-004
    @Test
    void register_futureBirthDate_throwsBusinessRuleAndNeverSaves() {
        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(false);
        RegisterUserRequest futureBirth = new RegisterUserRequest("andrea", "andrea@email.com",
                "Andrea", "Gómez", "3001234567", "Santa Marta", LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> userService.register(futureBirth))
                .isInstanceOf(BusinessRuleException.class);

        verify(userRepository, never()).save(any());
    }
}