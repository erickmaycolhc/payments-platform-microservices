package com.service.notification.dto.notification;

import com.service.notification.entity.NotificationEntity;
import com.service.notification.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        Long id,
        String accountNumber,
        UUID transferId,
        NotificationType type,
        String message,
        LocalDateTime createdAt
) {

    public static NotificationResponse fromEntity(NotificationEntity entity) {
        return new NotificationResponse(
            entity.getId(),
            entity.getAccountNumber(),
            entity.getTransferId(),
            entity.getType(),
            entity.getMessage(),
            entity.getCreatedAt()
        );
    }
}
