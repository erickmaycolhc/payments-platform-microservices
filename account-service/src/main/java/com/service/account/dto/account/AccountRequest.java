package com.service.account.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record AccountRequest(

        @NotNull
        UUID customerId,

        @NotBlank
        @Pattern(regexp = "PEN|USD", message = "The currency must be PEN or USD")
        String currency
)
{}
