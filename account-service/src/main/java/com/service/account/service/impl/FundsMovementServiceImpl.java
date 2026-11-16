package com.service.account.service.impl;

import com.service.account.dto.event.FundsFailedPayload;
import com.service.account.dto.event.FundsMovedPayload;
import com.service.account.dto.event.TransferInitiatedPayload;
import com.service.account.entity.AccountEntity;
import com.service.account.entity.AccountMovementEntity;
import com.service.account.entity.ProcessedMessageEntity;
import com.service.account.enums.AccountStatus;
import com.service.account.enums.MovementType;
import com.service.account.messaging.OutboxEventRecorder;
import com.service.account.repository.AccountMovementRepository;
import com.service.account.repository.AccountRepository;
import com.service.account.repository.ProcessedMessageRepository;
import com.service.account.service.FundsMovementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class FundsMovementServiceImpl implements FundsMovementService {

    private final ProcessedMessageRepository processedMessageRepository;
    private final AccountRepository accountRepository;
    private final OutboxEventRecorder outboxEventRecorder;
    private final AccountMovementRepository accountMovementRepository;

    @Override
    @Transactional
    public void process (String eventId, TransferInitiatedPayload payload) {

        if(processedMessageRepository.existsById(eventId)) {
            log.info("Event {} already processed, skipping", eventId);
        }

        AccountEntity source = accountRepository.findByAccountNumber(payload.sourceAccount()).orElse(null);
        AccountEntity target = accountRepository.findByAccountNumber(payload.targetAccount()).orElse(null);

        String failureReason = validate(source, target, payload);

        if(failureReason != null) {
            log.warn("Transfer {} rejected: {}", payload.transferId(), failureReason);
            outboxEventRecorder.record(payload.transferId(), "FundsFailed",
                    new FundsFailedPayload(payload.transferId(), failureReason));
        } else {
            moveFunds(source, target, payload);
            outboxEventRecorder.record(payload.transferId(), "FundsMoved",
                    new FundsMovedPayload(payload.transferId(), payload.sourceAccount(),
                            payload.targetAccount(), payload.amount(), payload.currency()));
        }

        processedMessageRepository.save(ProcessedMessageEntity.builder()
                .messageId(eventId)
                .processedAt(LocalDateTime.now())
                .build()
        );
    }

    private String validate(AccountEntity source, AccountEntity target, TransferInitiatedPayload payload) {
        if (payload.amount().signum() <= 0) {
            return "Invalid amount";
        }
        if (payload.sourceAccount().equals(payload.targetAccount())) {
            return "Source and target accounts must be different";
        }
        if (source == null) {
            return "Source account not found";
        }
        if (target == null) {
            return "Target account not found";
        }
        if (source.getStatus() != AccountStatus.ACTIVE) {
            return "Source account is not active";
        }
        if (target.getStatus() != AccountStatus.ACTIVE) {
            return "Target account is not active";
        }
        if (!source.getCurrency().equals(payload.currency())
                || !target.getCurrency().equals(payload.currency())) {
            return "Account currency does not match the transfer currency";
        }
        if (source.getBalance().compareTo(payload.amount()) < 0) {
            return "Insufficient funds";
        }
        return null;
    }

    private void moveFunds(AccountEntity source, AccountEntity target, TransferInitiatedPayload payload) {
        BigDecimal newSourceBalance = source.getBalance().subtract(payload.amount());
        BigDecimal newTargetBalance = target.getBalance().add(payload.amount());

        source.setBalance(newSourceBalance);
        target.setBalance(newTargetBalance);

        accountRepository.save(source);
        accountRepository.save(target);

        LocalDateTime now = LocalDateTime.now();

        accountMovementRepository.save(AccountMovementEntity.builder()
                .account(source)
                .transferId(payload.transferId())
                .type(MovementType.DEBIT)
                .amount(payload.amount())
                .balanceAfter(newSourceBalance)
                .createdAt(now)
                .build()
        );

        accountMovementRepository.save(AccountMovementEntity.builder()
                .account(target)
                .transferId(payload.transferId())
                .type(MovementType.CREDIT)
                .amount(payload.amount())
                .balanceAfter(newTargetBalance)
                .createdAt(now)
                .build()
        );
    }
}
