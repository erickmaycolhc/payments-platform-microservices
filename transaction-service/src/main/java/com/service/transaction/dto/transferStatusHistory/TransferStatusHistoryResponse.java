package com.service.transaction.dto.transferStatusHistory;

import com.service.transaction.entity.TransferStatusHistoryEntity;
import com.service.transaction.enums.TransferStatus;

import java.time.LocalDateTime;

public record TransferStatusHistoryResponse(
        TransferStatus status,
        String reason,
        LocalDateTime changeAt
) {
    public static TransferStatusHistoryResponse fromEntity(TransferStatusHistoryEntity entity) {
        return new TransferStatusHistoryResponse(
                entity.getStatus(),
                entity.getReason(),
                entity.getChangeAt()
        );
    }
}
