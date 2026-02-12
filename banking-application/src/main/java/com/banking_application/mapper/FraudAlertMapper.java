package com.banking_application.mapper;

import com.banking_application.dto.FraudAlertResponseDto;
import com.banking_application.model.FraudAlert;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FraudAlertMapper {

    @Mapping(target = "transactionReference", source = "transaction.transactionReference")
    @Mapping(target = "username", source = "user.username")
    FraudAlertResponseDto toDto(FraudAlert alert);

    List<FraudAlertResponseDto> toDto(List<FraudAlert> alerts);
}
