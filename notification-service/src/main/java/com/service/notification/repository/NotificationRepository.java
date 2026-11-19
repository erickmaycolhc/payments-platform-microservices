package com.service.notification.repository;


import com.service.notification.entity.NotificationEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificationRepository extends ReactiveCrudRepository<NotificationEntity, Long> {

    Mono<Boolean> existsByEventIdAndAccountNumber(String evenId, String accountNumber);
    Flux<NotificationEntity> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
}
