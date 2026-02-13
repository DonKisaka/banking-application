package com.banking_application.mapper;

import com.banking_application.dto.TransactionResponse;
import com.banking_application.dto.TransferRequestDto;
import com.banking_application.model.Account;
import com.banking_application.model.Transaction;
import com.banking_application.model.TransactionStatus;
import com.banking_application.model.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-13T11:10:00+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 25.0.1 (Oracle Corporation)"
)
@Component
public class TransactionMapperImpl implements TransactionMapper {

    @Override
    public Transaction toEntity(TransferRequestDto request) {
        if ( request == null ) {
            return null;
        }

        Transaction.TransactionBuilder transaction = Transaction.builder();

        transaction.amount( request.amount() );
        transaction.description( request.description() );

        transaction.transactionStatus( TransactionStatus.PENDING );
        transaction.transactionType( TransactionType.TRANSFER );

        return transaction.build();
    }

    @Override
    public TransactionResponse toDto(Transaction transaction) {
        if ( transaction == null ) {
            return null;
        }

        String sourceAccountNumber = null;
        String targetAccountNumber = null;
        String transactionReference = null;
        BigDecimal amount = null;
        String currency = null;
        String description = null;

        sourceAccountNumber = transactionSourceAccountAccountNumber( transaction );
        targetAccountNumber = transactionTargetAccountAccountNumber( transaction );
        transactionReference = transaction.getTransactionReference();
        amount = transaction.getAmount();
        currency = transaction.getCurrency();
        description = transaction.getDescription();

        TransactionType type = null;
        TransactionStatus status = null;
        LocalDateTime timestamp = null;

        TransactionResponse transactionResponse = new TransactionResponse( transactionReference, amount, currency, type, status, timestamp, description, sourceAccountNumber, targetAccountNumber );

        return transactionResponse;
    }

    @Override
    public List<TransactionResponse> toDto(List<Transaction> transactions) {
        if ( transactions == null ) {
            return null;
        }

        List<TransactionResponse> list = new ArrayList<TransactionResponse>( transactions.size() );
        for ( Transaction transaction : transactions ) {
            list.add( toDto( transaction ) );
        }

        return list;
    }

    private String transactionSourceAccountAccountNumber(Transaction transaction) {
        Account sourceAccount = transaction.getSourceAccount();
        if ( sourceAccount == null ) {
            return null;
        }
        return sourceAccount.getAccountNumber();
    }

    private String transactionTargetAccountAccountNumber(Transaction transaction) {
        Account targetAccount = transaction.getTargetAccount();
        if ( targetAccount == null ) {
            return null;
        }
        return targetAccount.getAccountNumber();
    }
}
