package com.service.transaction.service.impl;

import com.service.transaction.dto.event.TransferInitiatedPayload;
import com.service.transaction.dto.transfer.TransferRequest;
import com.service.transaction.dto.transfer.TransferResponse;
import com.service.transaction.dto.transferStatusHistory.TransferStatusHistoryResponse;
import com.service.transaction.entity.OutboxEventEntity;
import com.service.transaction.entity.TransferEntity;
import com.service.transaction.entity.TransferStatusHistoryEntity;
import com.service.transaction.enums.TransferStatus;
import com.service.transaction.exception.custom.BusinessRuleException;
import com.service.transaction.exception.custom.ResourceNotFoundException;
import com.service.transaction.repository.OutboxEventRepository;
import com.service.transaction.repository.TransferRepository;
import com.service.transaction.repository.TransferStatusHistoryRepository;
import com.service.transaction.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final TransferStatusHistoryRepository historyRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    @Override
    @Transactional
    public TransferResponse createTransfer(String idempotencyKey, TransferRequest request) {
        // verify duplicate idempotency key
        Optional<TransferEntity> existing = transferRepository.findByIdempotencyKey(idempotencyKey);

        if(existing.isPresent()){
            return TransferResponse.fromEntity(existing.get());
        }

        if(request.sourceAccount().equals(request.targetAccount())){
            throw new BusinessRuleException("The source account and the destination account must be different");
        }

        LocalDateTime now = LocalDateTime.now();

        // save transfer
        TransferEntity transfer = transferRepository.save(
                    TransferEntity.builder()
                            .idempotencyKey(idempotencyKey)
                            .sourceAccount(request.sourceAccount())
                            .targetAccount(request.targetAccount())
                            .amount(request.amount())
                            .currency(request.currency())
                            .description(request.description())
                            .status(TransferStatus.PENDING)
                            .createdAt(now)
                            .updatedAt(now)
                            .build());

        recordStatus(transfer, TransferStatus.PENDING, null, now); // save history transfer
        saveOutboxEvent(transfer, now); // send event

        return TransferResponse.fromEntity(transfer);
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse getById(UUID id) {
        TransferEntity entity = transferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + id));

        return TransferResponse.fromEntity(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferStatusHistoryResponse> getHistory(UUID id) {
        if(!transferRepository.existsById(id)){
            throw new ResourceNotFoundException("Transfer not found: " + id);
        }

        return historyRepository.findByTransferIdOrderByChangeAtAsc(id).stream()
                .map(TransferStatusHistoryResponse::fromEntity)
                .toList();
    }

    private void recordStatus (TransferEntity transfer, TransferStatus status, String reason, LocalDateTime changedAt){
        historyRepository.save(TransferStatusHistoryEntity.builder()
                .transfer(transfer)
                .status(status)
                .reason(reason)
                .changeAt(changedAt)
                .build()
        );
    }

    private void saveOutboxEvent (TransferEntity transfer, LocalDateTime createdAt){
        TransferInitiatedPayload payload = new TransferInitiatedPayload(
                transfer.getId(),
                transfer.getSourceAccount(),
                transfer.getTargetAccount(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getDescription()
        );

        outboxEventRepository.save(OutboxEventEntity.builder()
                .aggregateId(transfer.getId())
                .eventType("TransferInitiated")
                .payload(jsonMapper.writeValueAsString(payload))
                .createdAt(createdAt)
                .build());
    }
}
