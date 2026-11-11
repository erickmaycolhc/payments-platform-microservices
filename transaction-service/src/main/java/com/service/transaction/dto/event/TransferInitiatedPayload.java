package com.service.transaction.dto.event;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferInitiatedPayload(
        UUID transferId,
        String sourceAccount,
        String targetAccount,
        BigDecimal amount,
        String currency,
        String description
) {
}
