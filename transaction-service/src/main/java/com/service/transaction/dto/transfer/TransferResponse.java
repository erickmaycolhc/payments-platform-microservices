package com.service.transaction.dto.transfer;

import com.service.transaction.entity.TransferEntity;
import com.service.transaction.enums.TransferStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransferResponse(
   UUID id,
   //String idempotencyKey,
   String sourceAccount,
   String targetAccount,
   BigDecimal amount,
   String currency,
   String description,
   TransferStatus status,
   String failureReason,
   LocalDateTime createdAt,
   LocalDateTime updatedAt
) {
    public static TransferResponse fromEntity(TransferEntity entity) {
        return new TransferResponse(
                entity.getId(),
                //entity.getIdempotencyKey(),
                entity.getSourceAccount(),
                entity.getTargetAccount(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getFailureReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
