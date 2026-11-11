package com.service.account.dto.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(

   @NotBlank
   @Size(max = 50)
   String documentType,

   @NotBlank
   @Size(max = 50)
   String documentNumber,

   @NotBlank
   @Size(max = 200)
   String fullName,

   @NotBlank
   @Email
   @Size(max = 255)
    String email
) {}
