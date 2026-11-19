package com.service.transaction.service.impl;

import com.service.transaction.dto.event.FundsFailedPayload;
import com.service.transaction.dto.event.FundsMovedPayload;
import com.service.transaction.dto.event.TransferCompletedPayload;
import com.service.transaction.dto.event.TransferFailedPayload;
import com.service.transaction.entity.ProcessedMessageEntity;
import com.service.transaction.entity.TransferEntity;
import com.service.transaction.entity.TransferStatusHistoryEntity;
import com.service.transaction.enums.TransferStatus;
import com.service.transaction.messaging.OutboxEventRecorder;
import com.service.transaction.repository.ProcessedMessageRepository;
import com.service.transaction.repository.TransferRepository;
import com.service.transaction.repository.TransferStatusHistoryRepository;
import com.service.transaction.service.TransferResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransferResultServiceImpl implements TransferResultService {

    private final OutboxEventRecorder outboxEventRecorder;
    private final TransferRepository transferRepository;
    private final TransferStatusHistoryRepository historyRepository;
    private final ProcessedMessageRepository processedMessageRepository;

    @Override
    @Transactional
    public void handleFundsMoved(String eventId, FundsMovedPayload payload) {

        TransferEntity transfer = findPendingTransfer(eventId, payload.transferId());
        if (transfer == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        changeStatus(transfer, TransferStatus.COMPLETED, null, now);

        outboxEventRecorder.record(
                transfer.getId(),
                "TransferCompleted",
                new TransferCompletedPayload(
                        transfer.getId(),
                        transfer.getSourceAccount(),
                        transfer.getTargetAccount(),
                        transfer.getAmount(),
                        transfer.getCurrency()
                ),
                now
        );

        markProcessed(eventId, now);
    }

    @Override
    @Transactional
    public void handleFundsFailed(String eventId, FundsFailedPayload payload) {

        TransferEntity transfer = findPendingTransfer(eventId, payload.transferId());

        if (transfer == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        changeStatus(transfer, TransferStatus.FAILED, payload.reason(), now);

        outboxEventRecorder.record(
                transfer.getId(),
                "TransferFailed",
                new TransferFailedPayload(
                        transfer.getId(),
                        transfer.getSourceAccount(),
                        transfer.getTargetAccount(),
                        transfer.getAmount(),
                        transfer.getCurrency(),
                        payload.reason()
                ),
                now
        );

        markProcessed(eventId, now);
    }

    private TransferEntity findPendingTransfer(String eventId, UUID transferId) {

        if (processedMessageRepository.existsById(eventId)) {
            log.info("Event {} already processed, skipping", eventId);
            return null;
        }

        TransferEntity transfer = transferRepository.findById(transferId).orElse(null);

        if (transfer == null || transfer.getStatus() != TransferStatus.PENDING) {
            log.warn("Event {} ignored: transfer {} not found or not PENDING", eventId, transferId);
            markProcessed(eventId, LocalDateTime.now());
            return null;
        }

        return transfer;
    }

    private void changeStatus(TransferEntity transfer, TransferStatus status, String reason, LocalDateTime now) {

        transfer.setStatus(status);
        transfer.setFailureReason(reason);
        transfer.setUpdatedAt(now);
        transferRepository.save(transfer);

        historyRepository.save(TransferStatusHistoryEntity.builder()
                .transfer(transfer)
                .status(status)
                .reason(reason)
                .changeAt(now)
                .build()
        );
    }

    private void markProcessed(String eventId, LocalDateTime now) {

        processedMessageRepository.save(ProcessedMessageEntity.builder()
                .messageId(eventId)
                .processedAt(now)
                .build()
        );
    }
}
