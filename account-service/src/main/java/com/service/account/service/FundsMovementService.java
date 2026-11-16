package com.service.account.service;

import com.service.account.dto.event.TransferInitiatedPayload;

public interface FundsMovementService {

    void process(String eventId, TransferInitiatedPayload payload);
}
