package com.pulsepass.security;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.domain.enums.RoleName;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserCredentialRepository userCredentialRepository;
    @Mock private UserRoleRepository userRoleRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private User user(boolean active) {
        User u = new User();
        u.setId(1L);
        u.setUsername("andrea");
        u.setEmail("andrea@email.com");
        u.setActive(active);
        return u;
    }

    @Test
    void loadsExistingUserWithRolesAndHash() {
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user(true)));
        UserCredential credential = new UserCredential();
        credential.setPasswordHash("hashed");
        when(userCredentialRepository.findByUserId(1L)).thenReturn(Optional.of(credential));
        when(userRoleRepository.findByUserId(1L)).thenReturn(List.of(new UserRole(1L, RoleName.USER)));

        UserDetails details = service.loadUserByUsername("andrea");

        assertThat(details.getPassword()).isEqualTo("hashed");
        assertThat(details.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void throwsWhenUserDoesNotExist() {
        when(userRepository.findByUsername("nope")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nope"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void reflectsInactiveStatus() {
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user(false)));
        UserCredential credential = new UserCredential();
        credential.setPasswordHash("hashed");
        when(userCredentialRepository.findByUserId(1L)).thenReturn(Optional.of(credential));
        when(userRoleRepository.findByUserId(1L)).thenReturn(List.of(new UserRole(1L, RoleName.USER)));

        UserDetails details = service.loadUserByUsername("andrea");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void loadsByEmailWhenUsernameDoesNotMatch() {
        when(userRepository.findByUsername("andrea@email.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(user(true)));
        UserCredential credential = new UserCredential();
        credential.setPasswordHash("hashed");
        when(userCredentialRepository.findByUserId(1L)).thenReturn(Optional.of(credential));
        when(userRoleRepository.findByUserId(1L)).thenReturn(List.of(new UserRole(1L, RoleName.USER)));

        UserDetails details = service.loadUserByUsername("andrea@email.com");

        assertThat(details.getUsername()).isEqualTo("andrea");
    }
}