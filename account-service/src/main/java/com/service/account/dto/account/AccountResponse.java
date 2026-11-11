package com.service.account.dto.account;

import com.service.account.entity.AccountEntity;
import com.service.account.enums.AccountStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        String currency,
        BigDecimal balance,
        AccountStatus status,
        UUID customerId,
        LocalDateTime createdAt
) {

    public static AccountResponse fromEntity(AccountEntity entity) {
        return new AccountResponse(
          entity.getId(),
                entity.getAccountNumber(),
                entity.getCurrency(),
                entity.getBalance(),
                entity.getStatus(),
                entity.getCustomer().getId(),
                entity.getCreatedAt()
        );
    }
}
