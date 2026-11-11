package com.service.transaction.dto.event;

import tools.jackson.databind.JsonNode;

public record EventEnvelope(
        String eventId,
        String eventType,
        String occurredAt,
        String correlationId,
        JsonNode payload
) {
}
