package com.service.transaction.dto.transfer;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record TransferRequest(

        @NotBlank(message = "Source account number is required")
        @Size(max = 50)
        String sourceAccount,

        @NotBlank(message = "Target account number is required")
        @Size(max = 50)
        String targetAccount,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "PEN|USD", message = "Currency must be PEN or USD")
        String currency,

        @Size(max = 255)
        String description
) {}
