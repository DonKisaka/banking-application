package com.banking_application.mapper;

import com.banking_application.dto.AccountResponseDto;
import com.banking_application.dto.CreateAccountRequestDto;
import com.banking_application.model.Account;
import com.banking_application.model.AccountStatus;
import com.banking_application.model.AccountType;
import java.math.BigDecimal;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-12T17:20:56+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 25.0.1 (Oracle Corporation)"
)
@Component
public class AccountMapperImpl implements AccountMapper {

    @Override
    public AccountResponseDto toDto(Account account) {
        if ( account == null ) {
            return null;
        }

        UUID accountUuid = null;
        String accountNumber = null;
        BigDecimal balance = null;
        String currency = null;
        AccountType accountType = null;

        accountUuid = account.getAccountUuid();
        accountNumber = account.getAccountNumber();
        balance = account.getBalance();
        currency = account.getCurrency();
        accountType = account.getAccountType();

        AccountStatus accountStatus = null;

        AccountResponseDto accountResponseDto = new AccountResponseDto( accountUuid, accountNumber, balance, currency, accountStatus, accountType );

        return accountResponseDto;
    }

    @Override
    public Account toEntity(CreateAccountRequestDto request) {
        if ( request == null ) {
            return null;
        }

        Account.AccountBuilder account = Account.builder();

        account.balance( request.initialDeposit() );
        account.accountType( request.accountType() );
        account.currency( request.currency() );

        account.status( AccountStatus.ACTIVE );

        return account.build();
    }
}
