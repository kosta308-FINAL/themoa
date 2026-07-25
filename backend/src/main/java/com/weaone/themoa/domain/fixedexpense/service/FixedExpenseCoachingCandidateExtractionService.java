package com.weaone.themoa.domain.fixedexpense.service;

import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseStatus;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseCoachingDismissRepository;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseRepository;
import com.weaone.themoa.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 고정지출 코칭 규칙 계층 — 후보 풀을 결정론적으로 좁힌다(기부/회비 카테고리·dismiss 제외). "월세·관리비·
 * 보험처럼 카테고리가 애매한 필수 지출"까지 가려내는 건 규칙으로 안전하게 못 하므로 {@link FixedExpenseCoachingLlmClient}가
 * 이름을 보고 판단한다 — 여기서는 그 판단이 필요 없는 명백한 경우(기부/회비)만 미리 뺀다.
 */
@Service
@RequiredArgsConstructor
public class FixedExpenseCoachingCandidateExtractionService {

    private static final int ANNUAL_MONTHS = 12;

    private final FixedExpenseRepository fixedExpenseRepository;
    private final FixedExpenseCoachingDismissRepository coachingDismissRepository;

    @Transactional(readOnly = true)
    public List<FixedExpenseCoachingCandidate> extractCandidates(Member member) {
        Set<Long> dismissedIds = coachingDismissRepository.findByMember_Id(member.getId()).stream()
                .map(dismiss -> dismiss.getFixedExpense().getId())
                .collect(Collectors.toSet());

        return fixedExpenseRepository.findByMember_IdAndStatus(member.getId(), FixedExpenseStatus.ACTIVE).stream()
                .filter(fixedExpense -> !CategoryCode.DONATION.name().equals(fixedExpense.getCategory().getCode()))
                .filter(fixedExpense -> !dismissedIds.contains(fixedExpense.getId()))
                .map(this::toCandidate)
                .toList();
    }

    private FixedExpenseCoachingCandidate toCandidate(FixedExpense fixedExpense) {
        // 환율 변환으로 소수점이 붙은 원화 금액을 그대로 넘기면 LLM이 문구에 그 소수점까지 echo한다 —
        // 원 단위는 소수점이 없는 게 자연스러워 여기서 정수로 반올림해 넘긴다.
        BigDecimal monthlyAmount = fixedExpense.getExpectedAmountKrw().setScale(0, RoundingMode.HALF_UP);
        BigDecimal annualAmount = monthlyAmount.multiply(BigDecimal.valueOf(ANNUAL_MONTHS));
        return new FixedExpenseCoachingCandidate(fixedExpense.getId(), fixedExpense.getName(),
                fixedExpense.getCategory().getName(), monthlyAmount, annualAmount);
    }
}
