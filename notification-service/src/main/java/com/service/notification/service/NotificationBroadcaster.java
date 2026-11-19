package com.service.notification.service;

import com.service.notification.entity.NotificationEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@Component
public class NotificationBroadcaster {

    private final Sinks.Many<NotificationEntity> sink = Sinks.many().multicast().directBestEffort();

    public void publish(NotificationEntity notification) {
        sink.emitNext(notification, Sinks.EmitFailureHandler.busyLooping(Duration.ofMillis(100)));
    }

    public Flux<NotificationEntity> stream() {
        return sink.asFlux();
    }
}
