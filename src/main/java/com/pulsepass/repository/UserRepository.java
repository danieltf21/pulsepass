package com.pulsepass.repository;

import com.pulsepass.domain.User;
import org.hibernate.internal.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByUsername(String username);
}