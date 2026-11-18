package com.service.transaction.dto.event;

import java.util.UUID;

public record FundsFailedPayload(
        UUID transferId,
        String reason
) {
}
