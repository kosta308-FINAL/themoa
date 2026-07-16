package com.weaone.themoa.domain.budget.service;

import com.weaone.themoa.domain.budget.entity.Budget;
import com.weaone.themoa.domain.budget.entity.SurplusFund;
import com.weaone.themoa.domain.budget.repository.BudgetRepository;
import com.weaone.themoa.domain.budget.repository.SurplusFundRepository;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주기 마감 잉여금 적립 배치(dailyBudget.md §4, MOA-S-BUD-BGT-11). 종료일이 지난 주기의
 * {@code amount = 월 예산 − 그 주기 실제 지출}을 {@code surplus_fund}에 남긴다. 초과지출한 주기는 음수
 * 그대로 저장한다. UNIQUE(member_id, year_month) + 사전 존재 확인으로 주기당 정확히 1회만 적립한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SurplusFundBatchService {

    private final BudgetRepository budgetRepository;
    private final SurplusFundRepository surplusFundRepository;
    private final CardTransactionRepository cardTransactionRepository;

    /** 알림 배치(04:00) 이후 정리성으로 04:30에 돈다. */
    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    public void runNightlyAccrual() {
        LocalDate today = LocalDate.now(BudgetCyclePolicy.ZONE_SEOUL);
        List<Budget> endedCycles = budgetRepository.findByCycleEndDateBefore(today);
        for (Budget budget : endedCycles) {
            try {
                accrueIfAbsent(budget, today);
            } catch (RuntimeException e) {
                log.warn("잉여금 적립 1건 실패, 다음 주기로 계속 진행합니다. budgetId={}", budget.getId(), e);
            }
        }
    }

    @Transactional
    public void accrueIfAbsent(Budget budget, LocalDate today) {
        Long memberId = budget.getMember().getId();
        if (surplusFundRepository.existsByMember_IdAndYearMonth(memberId, budget.getYearMonth())) {
            return;
        }
        BigDecimal spentThisCycle = cardTransactionRepository.sumNetSpend(
                memberId, TransactionStatus.REJECTED, budget.getCycleStartDate(), budget.getCycleEndDate());
        BigDecimal amount = budget.getRemainingAmount(spentThisCycle); // 음수 그대로 — 0으로 깎지 않는다
        try {
            surplusFundRepository.save(SurplusFund.accrue(
                    budget.getMember(), budget.getYearMonth(), amount, today, LocalDateTime.now()));
        } catch (DataIntegrityViolationException e) {
            // UNIQUE 경합(배치 중복 실행) — 이미 적립됐으므로 조용히 지나간다(멱등).
            log.debug("잉여금 UNIQUE 경합, 기존 적립을 유지합니다. budgetId={}", budget.getId());
        }
    }
}
