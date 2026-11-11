package com.service.account.dto.customer;

import com.service.account.entity.CustomerEntity;
import com.service.account.enums.CustomerStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String documentType,
        String documentNumber,
        String fullName,
        String email,
        CustomerStatus status,
        LocalDateTime createdAt
) {

    public static CustomerResponse fromEntity(CustomerEntity entity) {
        return new CustomerResponse(
                entity.getId(),
                entity.getDocumentType(),
                entity.getDocumentNumber(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
