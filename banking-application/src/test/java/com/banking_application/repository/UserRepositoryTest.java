package com.banking_application.repository;

import com.banking_application.model.User;
import com.banking_application.model.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository underTest;

    @AfterEach
    void tearDown() {
        underTest.deleteAll();
    }

    @Test
    void itShouldFindUserByUsername() {
        // given
        String username = "donaldkisaka";
        User user = User.builder()
                .userUuid(UUID.randomUUID())
                .username(username)
                .email("kisakadonald@example.com")
                .password("securePassword123")
                .phoneNumber("+254745623541")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        underTest.save(user);

        // when
        Optional<User> result = underTest.findByUsername(username);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo(username);
    }

    @Test
    void itShouldNotFindUserByUsernameWhenDoesNotExist() {
        // given
        String username = "nonexistent";

        // when
        Optional<User> result = underTest.findByUsername(username);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void itShouldFindUserByUserUuid() {
        // given
        UUID userUuid = UUID.randomUUID();
        User user = User.builder()
                .userUuid(userUuid)
                .username("davidkisaka")
                .email("davidkisaka@example.com")
                .password("securePassword456")
                .phoneNumber("+254712569002")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        underTest.save(user);

        // when
        Optional<User> result = underTest.findByUserUuid(userUuid);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUserUuid()).isEqualTo(userUuid);
    }

    @Test
    void itShouldNotFindUserByUserUuidWhenDoesNotExist() {
        // given
        UUID nonExistentUuid = UUID.randomUUID();

        // when
        Optional<User> result = underTest.findByUserUuid(nonExistentUuid);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void itShouldCheckWhenUsernameOrEmailExists() {
        // given
        String username = "tedotengo";
        String email = "tedotengo@example.com";
        User user = User.builder()
                .userUuid(UUID.randomUUID())
                .username(username)
                .email(email)
                .password("password123")
                .phoneNumber("+254723994101")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        underTest.save(user);

        // when
        boolean existsByUsername = underTest.existsByUsernameOrEmail(username, "different@email.com");
        boolean existsByEmail = underTest.existsByUsernameOrEmail("differentuser", email);
        boolean existsByBoth = underTest.existsByUsernameOrEmail(username, email);

        // then
        assertThat(existsByUsername).isTrue();
        assertThat(existsByEmail).isTrue();
        assertThat(existsByBoth).isTrue();
    }

    @Test
    void itShouldCheckWhenUsernameAndEmailDoNotExist() {
        // given
        String username = "nonexistent";
        String email = "nonexistent@example.com";

        // when
        boolean exists = underTest.existsByUsernameOrEmail(username, email);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    void itShouldCheckWhenUserUuidExists() {
        // given
        UUID userUuid = UUID.randomUUID();
        User user = User.builder()
                .userUuid(userUuid)
                .username("lisamukoya")
                .email("lisamukoya@example.com")
                .password("password789")
                .phoneNumber("+254707412258")
                .role(UserRole.ADMIN)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        underTest.save(user);

        // when
        boolean exists = underTest.existsByUserUuid(userUuid);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    void itShouldCheckWhenUserUuidDoesNotExist() {
        // given
        UUID nonExistentUuid = UUID.randomUUID();

        // when
        boolean exists = underTest.existsByUserUuid(nonExistentUuid);

        // then
        assertThat(exists).isFalse();
    }
}
