package com.service.transaction.messaging;

import com.service.transaction.dto.event.EventEnvelope;
import com.service.transaction.dto.event.FundsFailedPayload;
import com.service.transaction.dto.event.FundsMovedPayload;
import com.service.transaction.service.TransferResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@Slf4j
@RequiredArgsConstructor
public class AccountEventsListener {

    private final JsonMapper jsonMapper;
    private final TransferResultService transferResultService;

    @KafkaListener(topics = "account.events")
    public void onAccountEvent(ConsumerRecord<String, String> record) {

        EventEnvelope envelope = jsonMapper.readValue(record.value(), EventEnvelope.class);

        switch (envelope.eventType()) {
            case "FundsMoved" -> transferResultService.handleFundsMoved(
                    envelope.eventId(),
                    jsonMapper.treeToValue(envelope.payload(), FundsMovedPayload.class));
            case "FundsFailed" -> transferResultService.handleFundsFailed(
                    envelope.eventId(),
                    jsonMapper.treeToValue(envelope.payload(), FundsFailedPayload.class));
            default -> log.debug("Ignoring event {} ({})", envelope.eventId(), envelope.eventType());
        }

    }
}
