package com.banking_application.mapper;

import com.banking_application.dto.AccountResponseDto;
import com.banking_application.dto.CreateAccountRequestDto;
import com.banking_application.model.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    AccountResponseDto toDto(Account account);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "balance", source = "initialDeposit")
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "accountUuid", ignore = true)
    @Mapping(target = "accountNumber", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "interestRate", ignore = true)
    @Mapping(target = "minimumBalance", ignore = true)
    @Mapping(target = "overDraftLimit", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Account toEntity(CreateAccountRequestDto request);
}
