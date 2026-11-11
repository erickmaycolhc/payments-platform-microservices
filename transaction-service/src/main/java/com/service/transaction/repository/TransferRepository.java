package com.service.transaction.repository;

import com.service.transaction.entity.TransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<TransferEntity, UUID> {

    Optional<TransferEntity> findByIdempotencyKey(String idempotencyKey);
}
