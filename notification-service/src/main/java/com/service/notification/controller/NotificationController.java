package com.service.notification.controller;

import com.service.notification.dto.notification.NotificationResponse;
import com.service.notification.repository.NotificationRepository;
import com.service.notification.service.NotificationBroadcaster;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final NotificationRepository notificationRepository;
    private final NotificationBroadcaster notificationBroadcaster;

    @GetMapping
    public Flux<NotificationResponse> getHistory(@RequestParam String account, @RequestParam(defaultValue = "50") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);

        return notificationRepository.findByAccountNumberOrderByCreatedAtDesc(account)
                .take(safeLimit)
                .map(NotificationResponse::fromEntity);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<NotificationResponse>> stream(@RequestParam String account) {

        Flux<ServerSentEvent<NotificationResponse>> events = notificationBroadcaster.stream()
                .filter(notification -> account.equals(notification.getAccountNumber()))
                .map(notification -> ServerSentEvent.<NotificationResponse>builder()
                        .id(String.valueOf(notification.getId()))
                        .event("notification")
                        .data(NotificationResponse.fromEntity(notification))
                        .build());

        Flux<ServerSentEvent<NotificationResponse>> heartbeat = Flux.interval(Duration.ofSeconds(25))
                .map(tick -> ServerSentEvent.<NotificationResponse>builder().comment("keep-alive").build());

        return Flux.merge(events, heartbeat);
    }
}
