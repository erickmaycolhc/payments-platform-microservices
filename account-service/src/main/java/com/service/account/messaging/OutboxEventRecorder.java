package com.service.account.messaging;

import com.service.account.entity.OutboxEventEntity;
import com.service.account.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxEventRecorder {

    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    public void record(UUID aggregateId, String eventType, Object payload) {
        outboxEventRepository.save(OutboxEventEntity.builder()
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(jsonMapper.writeValueAsString(payload))
                .createdAt(LocalDateTime.now())
                .build()
        );
    }
}
