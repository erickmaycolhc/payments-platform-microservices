package com.service.transaction.repository;

import com.service.transaction.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    List<OutboxEventEntity> findTop50ByPublishedAtIsNullOrderByIdAsc();
}
