package com.service.transaction.service;

import com.service.transaction.dto.transfer.TransferRequest;
import com.service.transaction.dto.transfer.TransferResponse;
import com.service.transaction.dto.transferStatusHistory.TransferStatusHistoryResponse;

import java.util.List;
import java.util.UUID;

public interface TransferService {

    TransferResponse createTransfer(String idempotencyKey, TransferRequest request);

    TransferResponse getById(UUID id);

    List<TransferStatusHistoryResponse> getHistory(UUID id);
}
