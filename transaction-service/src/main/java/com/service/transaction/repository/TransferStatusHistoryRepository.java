package com.service.transaction.repository;

import com.service.transaction.entity.TransferStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.swing.text.html.Option;
import java.util.Optional;
import java.util.UUID;

public interface TransferStatusHistoryRepository extends JpaRepository<TransferStatusHistoryEntity, Long> {

    Optional<TransferStatusHistoryEntity> findByTransferIdOrderByChangeAtAsc(UUID transferId);
}
