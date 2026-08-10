package com.alonsopisani.withdrawals.service;

import com.alonsopisani.withdrawals.domain.Withdrawal;
import com.alonsopisani.withdrawals.domain.WithdrawalStatus;
import com.alonsopisani.withdrawals.domain.WithdrawalStatusHistory;
import com.alonsopisani.withdrawals.repository.AccountRepository;
import com.alonsopisani.withdrawals.repository.WithdrawalRepository;
import com.alonsopisani.withdrawals.repository.WithdrawalStatusHistoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private static final String ACTOR_CLIENT = "CLIENT";

    private final AccountRepository accountRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final WithdrawalStatusHistoryRepository historyRepository;

    /**
     * Reserves the requested amount and creates the withdrawal in EVALUATING_RISK. The
     * reservation and the insert happen in the same transaction: if anything after the
     * reservation fails, the reservation itself rolls back too (no leaked reserved balance).
     */
    @Transactional
    public Withdrawal createWithdrawal(UUID accountId, String destinationCbu, BigDecimal amount) {
        int rowsReserved = accountRepository.tryReserve(accountId, amount);
        if (rowsReserved == 0) {
            if (!accountRepository.existsById(accountId)) {
                throw new EntityNotFoundException("Account not found: " + accountId);
            }
            throw new InsufficientBalanceException("Insufficient available balance for account " + accountId);
        }

        Instant now = Instant.now();
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setId(UUID.randomUUID());
        withdrawal.setAccountId(accountId);
        withdrawal.setDestinationCbu(destinationCbu);
        withdrawal.setAmount(amount);
        withdrawal.setStatus(WithdrawalStatus.EVALUATING_RISK);
        withdrawal.setIdempotencyKey(UUID.randomUUID());
        withdrawal.setCreatedAt(now);
        withdrawal.setUpdatedAt(now);
        withdrawal.setUpdatedBy(ACTOR_CLIENT);
        withdrawalRepository.save(withdrawal);

        historyRepository.save(new WithdrawalStatusHistory(
                UUID.randomUUID(), withdrawal.getId(), null, WithdrawalStatus.EVALUATING_RISK, ACTOR_CLIENT, now));

        return withdrawal;
    }
}
