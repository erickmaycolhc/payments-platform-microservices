package com.service.transaction.service;

import com.service.transaction.dto.transfer.TransferRequest;
import com.service.transaction.dto.transfer.TransferResponse;
import com.service.transaction.entity.OutboxEventEntity;
import com.service.transaction.entity.TransferEntity;
import com.service.transaction.entity.TransferStatusHistoryEntity;
import com.service.transaction.enums.TransferStatus;
import com.service.transaction.exception.custom.BusinessRuleException;
import com.service.transaction.repository.OutboxEventRepository;
import com.service.transaction.repository.TransferRepository;
import com.service.transaction.repository.TransferStatusHistoryRepository;
import com.service.transaction.service.impl.TransferServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransferServiceImplTest {

    private static final String IDEMPOTENCY_KEY = "6f1c2d9e-0b3a-4c55-9a7e-1d2f3a4b5c6d";
    private static final String SOURCE_ACCOUNT = "123456789012";
    private static final String TARGET_ACCOUNT = "210987654321";
    private static final UUID TRANSFER_ID = UUID.randomUUID();

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private TransferStatusHistoryRepository historyRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private JsonMapper jsonMapper;

    @InjectMocks
    private TransferServiceImpl transferService;

    private TransferRequest validRequest() {
        return new TransferRequest(
                SOURCE_ACCOUNT, TARGET_ACCOUNT, new BigDecimal("50.00"), "PEN", "Test transfer");
    }


    @Test
    void createTransfer_newKey_savesPendingTransferHistoryAndOutboxEvent() {
        // given
        when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(transferRepository.save(any(TransferEntity.class))).thenAnswer(inv -> {
            TransferEntity transfer = inv.getArgument(0);
            transfer.setId(TRANSFER_ID);
            return transfer;
        });
        when(jsonMapper.writeValueAsString(any())).thenReturn("{\"transferId\":\"test\"}");

        // when
        TransferResponse response = transferService.createTransfer(IDEMPOTENCY_KEY, validRequest());

        // then: response
        assertThat(response.id()).isEqualTo(TRANSFER_ID);
        assertThat(response.status()).isEqualTo(TransferStatus.PENDING);
        assertThat(response.amount()).isEqualByComparingTo("50.00");

        // then: transfer saved as PENDING with the idempotency key
        ArgumentCaptor<TransferEntity> transferCaptor = ArgumentCaptor.forClass(TransferEntity.class);
        verify(transferRepository).save(transferCaptor.capture());
        assertThat(transferCaptor.getValue().getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
        assertThat(transferCaptor.getValue().getStatus()).isEqualTo(TransferStatus.PENDING);

        // then: history row for PENDING
        ArgumentCaptor<TransferStatusHistoryEntity> historyCaptor =
                ArgumentCaptor.forClass(TransferStatusHistoryEntity.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getStatus()).isEqualTo(TransferStatus.PENDING);
        assertThat(historyCaptor.getValue().getTransfer().getId()).isEqualTo(TRANSFER_ID);

        // then: outbox event for TransferInitiated
        ArgumentCaptor<OutboxEventEntity> outboxCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("TransferInitiated");
        assertThat(outboxCaptor.getValue().getAggregateId()).isEqualTo(TRANSFER_ID);
        assertThat(outboxCaptor.getValue().getPayload()).isEqualTo("{\"transferId\":\"test\"}");
    }

    @Test
    void createTransfer_existingKey_returnsExistingWithoutCreatingNew() {
        // given
        TransferEntity existing = TransferEntity.builder()
                .id(TRANSFER_ID)
                .idempotencyKey(IDEMPOTENCY_KEY)
                .sourceAccount(SOURCE_ACCOUNT)
                .targetAccount(TARGET_ACCOUNT)
                .amount(new BigDecimal("50.00"))
                .currency("PEN")
                .status(TransferStatus.PENDING)
                .build();
        when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(existing));

        // when
        TransferResponse response = transferService.createTransfer(IDEMPOTENCY_KEY, validRequest());

        // then
        assertThat(response.id()).isEqualTo(TRANSFER_ID);
        verify(transferRepository, never()).saveAndFlush(any());
        verifyNoInteractions(historyRepository, outboxEventRepository);
    }

    @Test
    void createTransfer_sameSourceAndTarget_throwsBusinessRuleAndDoesNotSave() {
        // given
        when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        TransferRequest request = new TransferRequest(
                SOURCE_ACCOUNT, SOURCE_ACCOUNT, new BigDecimal("50.00"), "PEN", "Test transfer");

        // when / then
        assertThatThrownBy(() -> transferService.createTransfer(IDEMPOTENCY_KEY, request))
                .isInstanceOf(BusinessRuleException.class);

        verify(transferRepository, never()).saveAndFlush(any());
        verifyNoInteractions(historyRepository, outboxEventRepository);
    }
}
