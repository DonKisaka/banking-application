package com.banking_application.mapper;

import com.banking_application.dto.AuditLogResponseDto;
import com.banking_application.model.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    @Mapping(target = "username", source = "user.username")
    AuditLogResponseDto toDto(AuditLog auditLog);

    List<AuditLogResponseDto> toDto(List<AuditLog> auditLogs);
}
