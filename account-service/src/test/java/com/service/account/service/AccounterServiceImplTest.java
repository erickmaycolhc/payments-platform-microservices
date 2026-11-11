package com.service.account.service;

import com.service.account.dto.account.DepositRequest;
import com.service.account.dto.accountMovement.AccountMovementResponse;
import com.service.account.entity.AccountEntity;
import com.service.account.entity.AccountMovementEntity;
import com.service.account.enums.AccountStatus;
import com.service.account.enums.MovementType;
import com.service.account.exception.custom.BusinessRuleException;
import com.service.account.exception.custom.ResourceNotFoundException;
import com.service.account.repository.AccountMovementRepository;
import com.service.account.repository.AccountRepository;
import com.service.account.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccounterServiceImplTest {

    private final static String ACCOUNT_NUMBER =  "123456789012";

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMovementRepository accountMovementRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private AccountEntity buildAccount(AccountStatus status, BigDecimal balance){
            return AccountEntity.builder()
                    .id(UUID.randomUUID())
                    .accountNumber(ACCOUNT_NUMBER)
                    .currency("PEN")
                    .balance(balance)
                    .status(status)
                    .build();
    }

    @Test
    void deposit_validAmount_increasesBalanceAndCreatesCreditMovement() {
        // given
        AccountEntity account = buildAccount(AccountStatus.ACTIVE, BigDecimal.ZERO);
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));
        when(accountMovementRepository.save(any(AccountMovementEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // when
        AccountMovementResponse response =
                accountService.deposit(ACCOUNT_NUMBER, new DepositRequest(new BigDecimal("100.00")));

        // then
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
        assertThat(response.type()).isEqualTo(MovementType.CREDIT);
        assertThat(response.amount()).isEqualByComparingTo("100.00");
        assertThat(response.balanceAfter()).isEqualByComparingTo("100.00");

        ArgumentCaptor<AccountMovementEntity> captor = ArgumentCaptor.forClass(AccountMovementEntity.class);
        verify(accountMovementRepository).save(captor.capture());
        assertThat(captor.getValue().getTransferId()).isNull();
    }

    @Test
    void deposit_inactiveAccount_throwsBusinessRuleAndDoesNotSave() {
        // given
        AccountEntity account = buildAccount(AccountStatus.BLOCKED, BigDecimal.ZERO);
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));

        // when / then
        assertThatThrownBy(() ->
                accountService.deposit(ACCOUNT_NUMBER, new DepositRequest(new BigDecimal("100.00"))))
                .isInstanceOf(BusinessRuleException.class);

        verify(accountMovementRepository, never()).save(any());
        assertThat(account.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void deposit_nonExistentAccount_throwsNotFound() {
        // given
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() ->
                accountService.deposit(ACCOUNT_NUMBER, new DepositRequest(new BigDecimal("100.00"))))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(accountMovementRepository);
    }
}
