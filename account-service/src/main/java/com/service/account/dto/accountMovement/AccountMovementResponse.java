package com.service.account.dto.accountMovement;

import com.service.account.entity.AccountMovementEntity;
import com.service.account.enums.MovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountMovementResponse(
        Long id,
        MovementType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        LocalDateTime createdAt
) {

    public static AccountMovementResponse fromEntity(AccountMovementEntity entity) {
        return new AccountMovementResponse(
                entity.getId(),
                entity.getType(),
                entity.getAmount(),
                entity.getBalanceAfter(),
                entity.getCreatedAt()
        );
    }
}