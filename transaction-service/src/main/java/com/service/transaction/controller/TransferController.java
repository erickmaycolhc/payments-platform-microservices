package com.service.transaction.controller;

import com.service.transaction.dto.transfer.TransferRequest;
import com.service.transaction.dto.transfer.TransferResponse;
import com.service.transaction.dto.transferStatusHistory.TransferStatusHistoryResponse;
import com.service.transaction.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse createTransfer (
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        return transferService.createTransfer(idempotencyKey, request);
    }

    @GetMapping("/{id}")
    public TransferResponse getById (@PathVariable UUID id) {
        return transferService.getById(id);
    }

    @GetMapping("/history/{id}")
    public List<TransferStatusHistoryResponse> getHistory (@PathVariable UUID id) {
        return transferService.getHistory(id);
    }
}
