package com.service.notification.dto.event;

import tools.jackson.databind.JsonNode;

public record EventEnvelope(
        String eventId,
        String eventType,
        String occurredAt,
        String correlationId,
        JsonNode payload
) {
}
