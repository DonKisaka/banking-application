package com.banking_application.mapper;

import com.banking_application.dto.AuditLogResponseDto;
import com.banking_application.model.AuditLog;
import com.banking_application.model.AuditStatus;
import com.banking_application.model.User;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-12T17:20:56+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 25.0.1 (Oracle Corporation)"
)
@Component
public class AuditLogMapperImpl implements AuditLogMapper {

    @Override
    public AuditLogResponseDto toDto(AuditLog auditLog) {
        if ( auditLog == null ) {
            return null;
        }

        String username = null;
        Long id = null;
        String action = null;
        String affectedResource = null;
        String ipAddress = null;
        AuditStatus outcome = null;
        String details = null;

        username = auditLogUserUsername( auditLog );
        id = auditLog.getId();
        action = auditLog.getAction();
        affectedResource = auditLog.getAffectedResource();
        ipAddress = auditLog.getIpAddress();
        outcome = auditLog.getOutcome();
        details = auditLog.getDetails();

        AuditLogResponseDto auditLogResponseDto = new AuditLogResponseDto( id, username, action, affectedResource, ipAddress, outcome, details );

        return auditLogResponseDto;
    }

    @Override
    public List<AuditLogResponseDto> toDto(List<AuditLog> auditLogs) {
        if ( auditLogs == null ) {
            return null;
        }

        List<AuditLogResponseDto> list = new ArrayList<AuditLogResponseDto>( auditLogs.size() );
        for ( AuditLog auditLog : auditLogs ) {
            list.add( toDto( auditLog ) );
        }

        return list;
    }

    private String auditLogUserUsername(AuditLog auditLog) {
        User user = auditLog.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getUsername();
    }
}
