package com.banking_application.repository;

import com.banking_application.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByUserUuid(UUID userUuid);

    boolean existsByUsernameOrEmail(String username, String email);

    boolean existsByUserUuid(UUID userUuid);
}
