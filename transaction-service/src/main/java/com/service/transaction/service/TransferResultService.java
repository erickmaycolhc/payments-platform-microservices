package com.service.transaction.service;

import com.service.transaction.dto.event.FundsFailedPayload;
import com.service.transaction.dto.event.FundsMovedPayload;

public interface TransferResultService {

    void handleFundsMoved(String eventId, FundsMovedPayload payload);

    void handleFundsFailed(String eventId, FundsFailedPayload payload);
}
