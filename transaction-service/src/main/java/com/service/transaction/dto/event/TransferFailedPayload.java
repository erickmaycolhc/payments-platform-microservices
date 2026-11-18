package com.service.transaction.dto.event;

import java.util.UUID;

public record TransferFailedPayload(
        UUID transferId,
        String reason
) {
}
