package com.banking_application.mapper;

import com.banking_application.dto.AccountResponseDto;
import com.banking_application.dto.CreateAccountRequestDto;
import com.banking_application.model.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    @Mapping(target = "userUuid", source = "user.userUuid")
    AccountResponseDto toDto(Account account);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "balance", source = "initialDeposit")
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "accountUuid", ignore = true)
    @Mapping(target = "accountNumber", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "interestRate", expression = "java(getDefaultInterestRate(request.accountType()))")
    @Mapping(target = "minimumBalance", expression = "java(getDefaultMinimumBalance(request.accountType()))")
    @Mapping(target = "overDraftLimit", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Account toEntity(CreateAccountRequestDto request);
}
