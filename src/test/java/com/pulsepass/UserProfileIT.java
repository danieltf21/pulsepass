package com.pulsepass;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.repository.UserProfileRepository;
import com.pulsepass.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
class UserProfileIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @PersistenceContext
    private EntityManager em;

    private UserProfile profile(User user, String firstName, String lastName) {
        UserProfile p = new UserProfile();
        p.setFirstName(firstName);
        p.setLastName(lastName);
        p.setPhone("3001234567");
        p.setCity("Santa Marta");
        p.setUser(user);
        return p;
    }

    @Test
    void persistsUserWithProfile() {
        User user = userRepository.save(TestData.user("andrea", "andrea@mail.com"));
        userProfileRepository.save(profile(user, "Andrea", "Gómez"));

        em.flush();
        em.clear();

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getProfile()).isNotNull();
        assertThat(reloaded.getProfile().getFirstName()).isEqualTo("Andrea");
    }

    @Test
    void rejectsSecondProfileForSameUser() {
        User user = userRepository.save(TestData.user("andrea", "andrea@mail.com"));
        userProfileRepository.saveAndFlush(profile(user, "Andrea", "Gómez"));

        assertThrows(DataIntegrityViolationException.class,
                () -> userProfileRepository.saveAndFlush(profile(user, "Otra", "Persona")));
    }

    @Test
    void rejectsDuplicateUsername() {
        userRepository.saveAndFlush(TestData.user("andrea", "andrea@mail.com"));

        assertThrows(DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(TestData.user("andrea", "otro@mail.com")));
    }

    @Test
    void rejectsDuplicateEmail() {
        userRepository.saveAndFlush(TestData.user("andrea", "andrea@mail.com"));

        assertThrows(DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(TestData.user("otro", "andrea@mail.com")));
    }

    @Test
    void findsUserByEmailIgnoringCase() {
        userRepository.save(TestData.user("andrea", "andrea@mail.com"));

        User found = userRepository.findByEmailIgnoreCase("ANDREA@MAIL.COM").orElseThrow();

        assertThat(found.getUsername()).isEqualTo("andrea");
    }

    @Test
    void findsUserByUsername() {
        userRepository.save(TestData.user("carlos", "carlos@mail.com"));

        assertThat(userRepository.findByUsername("carlos")).isPresent();
        assertThat(userRepository.findByUsername("no-existe")).isEmpty();
    }
}