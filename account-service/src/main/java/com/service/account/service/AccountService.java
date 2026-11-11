package com.service.account.service;

import com.service.account.dto.account.AccountRequest;
import com.service.account.dto.account.AccountResponse;
import com.service.account.dto.account.DepositRequest;
import com.service.account.dto.accountMovement.AccountMovementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface AccountService {

    AccountResponse createAccount(AccountRequest request);

    AccountResponse getByNumber(String accountNumber);

    AccountMovementResponse deposit(String accountNumber, DepositRequest request);

    Page<AccountMovementResponse> getMovements(String accountNumber, Pageable pageable);
}