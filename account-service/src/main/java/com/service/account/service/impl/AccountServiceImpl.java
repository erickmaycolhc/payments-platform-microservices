package com.service.account.service.impl;

import com.service.account.dto.account.AccountRequest;
import com.service.account.dto.account.AccountResponse;
import com.service.account.dto.account.DepositRequest;
import com.service.account.dto.accountMovement.AccountMovementResponse;
import com.service.account.entity.AccountEntity;
import com.service.account.entity.AccountMovementEntity;
import com.service.account.entity.CustomerEntity;
import com.service.account.enums.AccountStatus;
import com.service.account.enums.CustomerStatus;
import com.service.account.enums.MovementType;
import com.service.account.exception.custom.BusinessRuleException;
import com.service.account.exception.custom.ResourceNotFoundException;
import com.service.account.repository.AccountMovementRepository;
import com.service.account.repository.AccountRepository;
import com.service.account.repository.CustomerRepository;
import com.service.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Service
@Slf4j
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AccountMovementRepository accountMovementRepository;

    @Override
    @Transactional
    public AccountResponse createAccount(AccountRequest request) {
        // verify customer
        CustomerEntity customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found: " + request.customerId()));

        // verify customer active
        if(customer.getStatus() != CustomerStatus.ACTIVE){
            throw new BusinessRuleException("The account is not active");
        }

        AccountEntity entity = AccountEntity.builder()
                .customer(customer)
                .accountNumber(generateAccountNumber())
                .currency(request.currency())
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        return AccountResponse.fromEntity(accountRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getByNumber(String accountNumber) {
        AccountEntity account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with number: " + accountNumber));

        return AccountResponse.fromEntity(account);
    }

    @Override
    @Transactional
    public AccountMovementResponse deposit(String accountNumber, DepositRequest request) {
        // Verify account
        AccountEntity account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with number: " + accountNumber));

        // verify active
        if(account.getStatus() != AccountStatus.ACTIVE){
            throw new BusinessRuleException("The account is not active");
        }
        // verify amount <= 0
        if(request.amount() == null || request.amount().signum() <= 0){
            throw new BusinessRuleException("The amount must be greater than zero");
        }

        BigDecimal newBalance = account.getBalance().add(request.amount());
        account.setBalance(newBalance);

        AccountMovementEntity movement = AccountMovementEntity.builder()
                .account(account)
                .transferId(null)
                .type(MovementType.CREDIT)
                .amount(request.amount())
                .balanceAfter(newBalance)
                .createdAt(LocalDateTime.now())
                .build();

        accountRepository.save(account); // update balance
        AccountMovementEntity savedMovement = accountMovementRepository.save(movement);

        return AccountMovementResponse.fromEntity(savedMovement);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountMovementResponse> getMovements(String accountNumber, Pageable pageable) {
        // verify customer
        AccountEntity account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + accountNumber));

        return accountMovementRepository.findByAccountId(account.getId(), pageable)
                .map(AccountMovementResponse::fromEntity);

    }

    private String generateAccountNumber() {
        String number;
        do {
            number = String.format("%012d", RANDOM.nextLong(1_000_000_000_000L));
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }
}
