package com.banking_application.mapper;

import com.banking_application.dto.TransactionResponse;
import com.banking_application.dto.TransferRequestDto;
import com.banking_application.model.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "sourceAccount", ignore = true)
    @Mapping(target = "targetAccount", ignore = true)
    @Mapping(target = "transactionReference", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "failedReason", ignore = true)
    @Mapping(target = "transactionStatus", constant = "PENDING")
    @Mapping(target = "initiatedBy", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "transactionType", constant = "TRANSFER")
    @Mapping(target = "ipAddress", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "deviceInfo", ignore = true)
    @Mapping(target = "currency", constant = "KSH")
    Transaction toEntity(TransferRequestDto request);


    @Mapping(target = "sourceAccountNumber", source = "sourceAccount.accountNumber")
    @Mapping(target = "targetAccountNumber", source = "targetAccount.accountNumber")
    TransactionResponse toDto(Transaction transaction);

    List<TransactionResponse> toDto(List<Transaction> transactions);

}
