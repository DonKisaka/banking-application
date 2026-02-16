package com.banking_application.mapper;

import com.banking_application.dto.FraudAlertResponseDto;
import com.banking_application.model.FraudAlert;
import com.banking_application.model.FraudStatus;
import com.banking_application.model.Transaction;
import com.banking_application.model.User;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-16T19:42:26+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 25.0.1 (Oracle Corporation)"
)
@Component
public class FraudAlertMapperImpl implements FraudAlertMapper {

    @Override
    public FraudAlertResponseDto toDto(FraudAlert alert) {
        if ( alert == null ) {
            return null;
        }

        String transactionReference = null;
        String username = null;
        Long id = null;
        Integer riskScore = null;
        String reasonCode = null;
        FraudStatus status = null;

        transactionReference = alertTransactionTransactionReference( alert );
        username = alertUserUsername( alert );
        id = alert.getId();
        riskScore = alert.getRiskScore();
        reasonCode = alert.getReasonCode();
        status = alert.getStatus();

        FraudAlertResponseDto fraudAlertResponseDto = new FraudAlertResponseDto( id, transactionReference, username, riskScore, reasonCode, status );

        return fraudAlertResponseDto;
    }

    @Override
    public List<FraudAlertResponseDto> toDto(List<FraudAlert> alerts) {
        if ( alerts == null ) {
            return null;
        }

        List<FraudAlertResponseDto> list = new ArrayList<FraudAlertResponseDto>( alerts.size() );
        for ( FraudAlert fraudAlert : alerts ) {
            list.add( toDto( fraudAlert ) );
        }

        return list;
    }

    private String alertTransactionTransactionReference(FraudAlert fraudAlert) {
        Transaction transaction = fraudAlert.getTransaction();
        if ( transaction == null ) {
            return null;
        }
        return transaction.getTransactionReference();
    }

    private String alertUserUsername(FraudAlert fraudAlert) {
        User user = fraudAlert.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getUsername();
    }
}
