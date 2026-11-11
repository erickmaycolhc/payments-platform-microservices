package com.service.account.controller;

import com.service.account.dto.account.AccountRequest;
import com.service.account.dto.account.AccountResponse;
import com.service.account.dto.account.DepositRequest;
import com.service.account.dto.accountMovement.AccountMovementResponse;
import com.service.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@Valid @RequestBody AccountRequest request){
        return accountService.createAccount(request);
    }

    @GetMapping("/{accountNumber}")
    public AccountResponse getByNumber(@PathVariable String accountNumber){
        return accountService.getByNumber(accountNumber);
    }

    @PostMapping("/deposit/{accountNumber}")
    public AccountMovementResponse deposit(@Valid @PathVariable String accountNumber, @RequestBody DepositRequest request){
        return accountService.deposit(accountNumber, request);
    }

    @GetMapping("/movements/{accountNumber}")
    public Page<AccountMovementResponse> getMovements(@PathVariable String accountNumber, Pageable pageable){
        return accountService.getMovements(accountNumber, pageable);
    }
}
