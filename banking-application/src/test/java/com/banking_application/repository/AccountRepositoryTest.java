package com.banking_application.repository;

import com.banking_application.model.Account;
import com.banking_application.model.AccountStatus;
import com.banking_application.model.AccountType;
import com.banking_application.model.User;
import com.banking_application.model.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    private AccountRepository underTest;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userUuid(UUID.randomUUID())
                .username("donaldkisaka")
                .email("donaldkisaka@example.com")
                .password("password123")
                .phoneNumber("+254707412258")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        testUser = userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        underTest.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void itShouldFindAccountByAccountNumber() {
        // given
        String accountNumber = "ACC123456789";
        Account account = Account.builder()
                .accountUuid(UUID.randomUUID())
                .user(testUser)
                .accountNumber(accountNumber)
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("1000.00"))
                .currency("KSH")
                .status(AccountStatus.ACTIVE)
                .build();
        underTest.save(account);

        // when
        Optional<Account> result = underTest.findByAccountNumber(accountNumber);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getAccountNumber()).isEqualTo(accountNumber);
    }

    @Test
    void itShouldNotFindAccountByAccountNumberWhenDoesNotExist() {
        // given
        String accountNumber = "NONEXISTENT123";

        // when
        Optional<Account> found = underTest.findByAccountNumber(accountNumber);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void itShouldFindAllAccountsByUser() {
        // given
        Account savingsAccount = Account.builder()
                .accountUuid(UUID.randomUUID())
                .user(testUser)
                .accountNumber("SAVINGS001")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("5000.00"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .build();

        Account currentAccount = Account.builder()
                .accountUuid(UUID.randomUUID())
                .user(testUser)
                .accountNumber("CURRENT001")
                .accountType(AccountType.CURRENT)
                .balance(new BigDecimal("10000.00"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .build();

        underTest.save(savingsAccount);
        underTest.save(currentAccount);

        // when
        List<Account> accounts = underTest.findByUser(testUser);

        // then
        assertThat(accounts).hasSize(2);
    }

    @Test
    void itShouldReturnEmptyListWhenUserHasNoAccounts() {
        // given
        User newUser = User.builder()
                .userUuid(UUID.randomUUID())
                .username("newuser")
                .email("newuser@example.com")
                .password("password456")
                .phoneNumber("+0987654321")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        newUser = userRepository.save(newUser);

        // when
        List<Account> accounts = underTest.findByUser(newUser);

        // then
        assertThat(accounts).isEmpty();
    }

    @Test
    void itShouldFindAccountByAccountNumberWithLock() {
        // given
        String accountNumber = "LOCKED001";
        Account account = Account.builder()
                .accountUuid(UUID.randomUUID())
                .user(testUser)
                .accountNumber(accountNumber)
                .accountType(AccountType.CURRENT)
                .balance(new BigDecimal("2500.00"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .build();
        underTest.save(account);

        // when
        Optional<Account> found = underTest.findByAccountNumberWithLock(accountNumber);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getAccountNumber()).isEqualTo(accountNumber);
    }

    @Test
    void itShouldNotFindAccountByAccountNumberWithLockWhenDoesNotExist() {
        // given
        String accountNumber = "NONEXISTENT999";

        // when
        Optional<Account> found = underTest.findByAccountNumberWithLock(accountNumber);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void itShouldFindOnlyAccountsForSpecificUser() {
        // given
        User anotherUser = User.builder()
                .userUuid(UUID.randomUUID())
                .username("anotheruser")
                .email("another@example.com")
                .password("password789")
                .phoneNumber("+1112223333")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        anotherUser = userRepository.save(anotherUser);

        Account testUserAccount = Account.builder()
                .accountUuid(UUID.randomUUID())
                .user(testUser)
                .accountNumber("TEST001")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("1000.00"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .build();

        Account anotherUserAccount = Account.builder()
                .accountUuid(UUID.randomUUID())
                .user(anotherUser)
                .accountNumber("ANOTHER001")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("2000.00"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .build();

        underTest.save(testUserAccount);
        underTest.save(anotherUserAccount);

        // when
        List<Account> testUserAccounts = underTest.findByUser(testUser);
        List<Account> anotherUserAccounts = underTest.findByUser(anotherUser);

        // then
        assertThat(testUserAccounts).hasSize(1);
        assertThat(anotherUserAccounts).hasSize(1);
        assertThat(testUserAccounts.get(0).getAccountNumber()).isEqualTo("TEST001");
        assertThat(anotherUserAccounts.get(0).getAccountNumber()).isEqualTo("ANOTHER001");
    }
}
