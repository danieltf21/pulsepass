package com.pulsepass.services.impl;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.UserMapper;
import com.pulsepass.repository.UserProfileRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.services.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository,
                           UserProfileRepository userProfileRepository,
                           UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        // BR-USER-001
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already exists: " + request.username());
        }

        // BR-USER-002
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("Email already exists: " + request.email());
        }

        // BR-USER-005
        if (request.birthDate() != null && request.birthDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("Birth date cannot be in the future");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setActive(true); // BR-USER-003

        User savedUser = userRepository.save(user); // BR-USER-004 (misma transacción)

        UserProfile profile = new UserProfile();
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setPhone(request.phone());
        profile.setCity(request.city());
        profile.setBirthDate(request.birthDate());
        profile.setUser(savedUser);
        userProfileRepository.save(profile);

        savedUser.setProfile(profile);
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
