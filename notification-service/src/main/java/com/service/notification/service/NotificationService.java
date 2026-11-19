package com.service.notification.service;

import com.service.notification.enums.NotificationType;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface NotificationService {

    Mono<Void> notifyAccount(String eventId, UUID transferId, String accountNumber,
                             NotificationType type, String message);
}
