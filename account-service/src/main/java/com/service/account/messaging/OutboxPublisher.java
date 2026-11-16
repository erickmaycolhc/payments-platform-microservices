package com.service.account.messaging;

import com.service.account.dto.event.EventEnvelope;
import com.service.account.entity.OutboxEventEntity;
import com.service.account.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisher {

    private final static String TOPIC = "account.event";
    private final static String SOURCE= "account-service";

    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;


    @Scheduled(fixedDelay = 2000)
    public void publishPendingEvents() {

        List<OutboxEventEntity> pending = outboxEventRepository.findTop50ByPublishedAtIsNullOrderByIdAsc();

        for (OutboxEventEntity event : pending) {
            try {
                send(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("Could not publish outbox event {}: {}", event.getId(), e.getMessage());
                return;
            }

            event.setPublishedAt(LocalDateTime.now());
            outboxEventRepository.save(event);
            log.info("Published event {} ({})", event.getId(), event.getEventType());
        }
    }

    private void send(OutboxEventEntity event) throws Exception {
        EventEnvelope envelope = new EventEnvelope(
                SOURCE + "-" + event.getId(),
                event.getEventType(),
                event.getCreatedAt().toString(),
                event.getAggregateId().toString(),
                jsonMapper.readTree(event.getPayload())
        );

        ProducerRecord<String, String> record = new ProducerRecord<>(
                TOPIC,
                event.getAggregateId().toString(),
                jsonMapper.writeValueAsString(envelope)
        );

        record.headers().add("eventType", event.getEventType().getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);

    }
}
