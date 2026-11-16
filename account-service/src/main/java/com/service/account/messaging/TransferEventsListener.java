package com.service.account.messaging;

import com.service.account.dto.event.EventEnvelope;
import com.service.account.dto.event.TransferInitiatedPayload;
import com.service.account.service.FundsMovementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class TransferEventsListener {

    private final static String TRANSFER_INITIATED = "TransferInitiated";
    private final JsonMapper jsonMapper;
    private final FundsMovementService fundsMovementService;

    @KafkaListener(topics = "transfer.events")
    public void onTransferEvent(ConsumerRecord<String, String> record) {

        EventEnvelope envelope = jsonMapper.readValue(record.value(), EventEnvelope.class);

        if (!TRANSFER_INITIATED.equals(envelope.eventType())) {
            log.debug("Ignoring event {} ({})", envelope.eventId(), envelope.eventType());
            return;
        }

        TransferInitiatedPayload payload = jsonMapper.treeToValue(envelope.payload(), TransferInitiatedPayload.class);

        log.info("Received {} eventId={} transferId={} {} -> {} amount={} {} (partition={}, offset={})",
                envelope.eventType(), envelope.eventId(), payload.transferId(),
                payload.sourceAccount(), payload.targetAccount(),
                payload.amount(), payload.currency(),
                record.partition(), record.offset()
        );

        fundsMovementService.process(envelope.eventId(), payload);
    }
}
