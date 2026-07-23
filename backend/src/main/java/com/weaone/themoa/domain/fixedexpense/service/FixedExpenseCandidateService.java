package com.weaone.themoa.domain.fixedexpense.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.budget.service.BudgetCycleService;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseCandidate;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseCandidateStatus;
import com.weaone.themoa.domain.fixedexpense.entity.RecurringPaymentGroup;
import com.weaone.themoa.domain.fixedexpense.entity.UserMerchantPreference;
import com.weaone.themoa.domain.fixedexpense.entity.UserMerchantPreferenceType;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseCandidateRepository;
import com.weaone.themoa.domain.fixedexpense.repository.UserMerchantPreferenceRepository;
import com.weaone.themoa.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 탐지 후보에 대한 사용자 응답(fixedExpense.md §3, F-02). "등록"은 여기서 처리하지 않는다 —
 * 그 흐름은 {@link FixedExpenseRegistrationService#registerFromCandidate}가 담당한다(승인 후 F-03에서
 * 사용자가 세부값을 확정해야 하므로 이 서비스만으로 고정지출이 생기지 않는다).
 */
@Service
@RequiredArgsConstructor
public class FixedExpenseCandidateService {

    private final FixedExpenseCandidateRepository fixedExpenseCandidateRepository;
    private final UserMerchantPreferenceRepository userMerchantPreferenceRepository;
    private final BudgetCycleService budgetCycleService;

    @Transactional(readOnly = true)
    public List<FixedExpenseCandidate> listPending(Long memberId) {
        return fixedExpenseCandidateRepository
                .findByMember_IdAndStatusOrderByScoreDescIdDesc(memberId, FixedExpenseCandidateStatus.PENDING);
    }

    /** 거절 — 영구. 같은 alias(또는 biller)는 preferences로 다시 추천하지 않는다(§3). */
    @Transactional
    public void reject(Long memberId, Long candidateId) {
        FixedExpenseCandidate candidate = getOwnedCandidate(memberId, candidateId);
        candidate.reject();
        suppressCandidateTarget(candidate, UserMerchantPreferenceType.DO_NOT_RECOMMEND);
    }

    /** 나중에 — 이번 주기만 건너뛰고 다음 급여 주기에 재추천한다(§3). */
    @Transactional
    public void snooze(Long memberId, Long candidateId) {
        FixedExpenseCandidate candidate = getOwnedCandidate(memberId, candidateId);
        LocalDate today = LocalDate.now(FixedExpenseCyclePolicy.ZONE_SEOUL);
        budgetCycleService.ensurePaydayPromoted(candidate.getMember(), today);
        String currentYearMonth = budgetCycleService.resolveCycleForDate(candidate.getMember(), today).yearMonth();
        candidate.snooze(currentYearMonth);
    }

    /** 습관적 소비로 분류 — 고정지출 아님으로 영구 재분류한다(§3). */
    @Transactional
    public void reclassifyHabit(Long memberId, Long candidateId) {
        FixedExpenseCandidate candidate = getOwnedCandidate(memberId, candidateId);
        candidate.classifyHabit();
        suppressCandidateTarget(candidate, UserMerchantPreferenceType.RECLASSIFY_HABIT);
    }

    /**
     * 그룹 단위로 억제를 건다(fixedExpense.md §2) — alias·billerMerchant 단위로 걸면 같은 alias(또는
     * merchant) 아래 공존하는 별개 구독까지 함께 억제돼버린다(같은 서비스를 계정 두 개로 구독하는 경우 등).
     * 이름형·biller형 구분 없이 동일하게 처리되므로 승인 전 alias 유무를 따질 필요도 없어졌다.
     */
    private void suppressCandidateTarget(FixedExpenseCandidate candidate, UserMerchantPreferenceType type) {
        Member member = candidate.getMember();
        RecurringPaymentGroup group = candidate.getRecurringPaymentGroup();
        try {
            if (userMerchantPreferenceRepository
                    .existsByMember_IdAndRecurringPaymentGroup_IdAndPreferenceType(member.getId(), group.getId(), type)) {
                return;
            }
            userMerchantPreferenceRepository.save(UserMerchantPreference.createForGroup(member, group, type));
        } catch (DataIntegrityViolationException e) {
            // 동시 요청 경합 — 이미 다른 요청이 같은 설정을 저장했다.
        }
    }

    private FixedExpenseCandidate getOwnedCandidate(Long memberId, Long candidateId) {
        return fixedExpenseCandidateRepository.findByIdAndMember_Id(candidateId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FIXED_EXPENSE_CANDIDATE_NOT_FOUND));
    }
}
