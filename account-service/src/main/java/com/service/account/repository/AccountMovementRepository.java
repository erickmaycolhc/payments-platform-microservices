package com.service.account.repository;

import com.service.account.entity.AccountMovementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountMovementRepository extends JpaRepository<AccountMovementEntity, Long> {

    Page<AccountMovementEntity> findByAccountId(UUID accountId, Pageable pageable);
}
