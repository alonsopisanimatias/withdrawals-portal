package com.alonsopisani.withdrawals.repository;

import com.alonsopisani.withdrawals.domain.WithdrawalStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WithdrawalStatusHistoryRepository extends JpaRepository<WithdrawalStatusHistory, UUID> {
}
