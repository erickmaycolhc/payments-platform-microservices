package com.service.notification.service.impl;

import com.service.notification.entity.NotificationEntity;
import com.service.notification.enums.NotificationType;
import com.service.notification.repository.NotificationRepository;
import com.service.notification.service.NotificationBroadcaster;
import com.service.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationBroadcaster notificationBroadcaster;

    @Override
    public Mono<Void> notifyAccount(String eventId, UUID transferId, String accountNumber, NotificationType type, String message) {
        return notificationRepository.existsByEventIdAndAccountNumber(eventId, accountNumber)
                .filter(exists -> !exists)
                .flatMap(notExits -> notificationRepository.save(NotificationEntity.builder()
                        .eventId(eventId)
                        .accountNumber(accountNumber)
                        .transferId(transferId)
                        .type(type)
                        .message(message)
                        .createdAt(LocalDateTime.now())
                        .build()))
                .doOnNext(saved -> {
                        log.info("Notification {} saved for account {} ({})",
                        saved.getId(), saved.getAccountNumber(), saved.getType());
                        notificationBroadcaster.publish(saved);
                })
                .onErrorResume(DuplicateKeyException.class, e -> Mono.empty())
                .then();
    }
}
