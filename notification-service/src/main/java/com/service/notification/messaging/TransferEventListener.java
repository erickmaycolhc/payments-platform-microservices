package com.service.notification.messaging;

import com.service.notification.dto.event.EventEnvelope;
import com.service.notification.dto.event.TransferCompletedPayload;
import com.service.notification.dto.event.TransferFailedPayload;
import com.service.notification.dto.event.TransferInitiatedPayload;
import com.service.notification.enums.NotificationType;
import com.service.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@Slf4j
@RequiredArgsConstructor
public class TransferEventListener {

    private final NotificationService notificationService;
    private final JsonMapper jsonMapper;

    @KafkaListener(topics = "transfer.events")
    public void onTransferEvent(ConsumerRecord<String, String> record) {
        EventEnvelope envelope = jsonMapper.readValue(record.value(), EventEnvelope.class);

        switch (envelope.eventType()) {
            case "TransferInitiated" -> onInitiated(envelope);
            case "TransferCompleted" -> onCompleted(envelope);
            case "TransferFailed" -> onFailed(envelope);
        }
    }

    private void onInitiated(EventEnvelope envelope) {
        TransferInitiatedPayload p = jsonMapper.treeToValue(envelope.payload(), TransferInitiatedPayload.class);
        notificationService.notifyAccount(
                envelope.eventId(),
                p.transferId(),
                p.sourceAccount(),
                NotificationType.TRANSFER_INITIATED,
                "Your transfer of %s %s to account %s was received and is being processed"
                        .formatted(p.amount(), p.currency(), p.targetAccount())).block();
    }

    private void onCompleted(EventEnvelope envelope) {
        TransferCompletedPayload p = jsonMapper.treeToValue(envelope.payload(), TransferCompletedPayload.class);
        notificationService.notifyAccount(
                envelope.eventId(),
                p.transferId(),
                p.sourceAccount(),
                NotificationType.TRANSFER_COMPLETED,
                "Your transfer of %s %s to account %s was completed"
                        .formatted(p.amount(), p.currency(), p.targetAccount())).block();
        notificationService.notifyAccount(
                envelope.eventId(),
                p.transferId(),
                p.targetAccount(),
                NotificationType.FUNDS_RECEIVED,
                "You received %s %s from account %s"
                        .formatted(p.amount(), p.currency(), p.sourceAccount())).block();
    }

    private void onFailed(EventEnvelope envelope) {
        TransferFailedPayload p = jsonMapper.treeToValue(envelope.payload(), TransferFailedPayload.class);

        if (p.sourceAccount() == null) {
            log.warn("Event {} skipped: payload has no source account (old format)", envelope.eventId());
            return;
        }
        notificationService.notifyAccount(
                envelope.eventId(),
                p.transferId(),
                p.sourceAccount(),
                NotificationType.TRANSFER_FAILED,
                "Your transfer of %s %s to account %s failed: %s"
                        .formatted(p.amount(), p.currency(), p.targetAccount(), p.reason())).block();
    }
}
