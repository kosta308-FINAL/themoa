package com.weaone.themoa.domain.fixedexpense.support;

import com.weaone.themoa.domain.budget.service.BudgetCycleService;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.category.repository.CategoryRepository;
import com.weaone.themoa.domain.fixedexpense.dto.request.FixedExpenseCandidateRegisterRequest;
import com.weaone.themoa.domain.fixedexpense.dto.request.FixedExpenseDirectRegisterRequest;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseCandidate;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpensePaymentMethod;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseStatus;
import com.weaone.themoa.domain.fixedexpense.entity.RecurringPaymentGroup;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseCandidateRepository;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseRepository;
import com.weaone.themoa.domain.fixedexpense.repository.RecurringPaymentGroupRepository;
import com.weaone.themoa.domain.fixedexpense.service.FixedExpenseCoachingCardService;
import com.weaone.themoa.domain.fixedexpense.service.FixedExpenseCyclePolicy;
import com.weaone.themoa.domain.fixedexpense.service.FixedExpenseRegistrationService;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.member.support.MemberDemoSeeder;
import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 시연용 고정지출 6건(Claude 구독·쿠팡와우 멤버십·월세·관리비·보험료·휴대폰 요금) 시드
 * (member_id=1, email="test"). 구독 2건은 F-02(추천 승인) → F-03(프리필 확정) 화면에서 사용자가 등록한
 * 값 그대로를 {@link FixedExpenseRegistrationService#registerFromCandidate} 경로로 재현한다. 생활 고정지출
 * 4건은 카드 거래와 자동 매칭하지 않는 계좌이체형 직접 등록으로 만든다 — Apple 결제(6300원)는 아직
 * 이름을 안 지어 PENDING 후보로 남겨둔 실제 상태와 동일하게 손대지 않는다.
 *
 * <p>{@link CardTransactionDemoSeeder}(@Order 10)가 카드 거래내역을 먼저 수집해 이 두 건을 포함한 고정지출
 * 후보 3건을 탐지해 둬야 하므로 그보다 뒤에 돈다.
 */
@Slf4j
@Component
@Order(11)
@ConditionalOnProperty(prefix = "app.demo.fixed-expense", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class FixedExpenseDemoSeeder implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final FixedExpenseRepository fixedExpenseRepository;
    private final CategoryRepository categoryRepository;
    private final MerchantAliasRepository merchantAliasRepository;
    private final RecurringPaymentGroupRepository recurringPaymentGroupRepository;
    private final FixedExpenseCandidateRepository fixedExpenseCandidateRepository;
    private final FixedExpenseRegistrationService fixedExpenseRegistrationService;
    private final FixedExpenseCoachingCardService fixedExpenseCoachingCardService;
    private final BudgetCycleService budgetCycleService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Member member = memberRepository.findByEmail(MemberDemoSeeder.DEMO_EMAIL).orElse(null);
        if (member == null) {
            return;
        }

        if (fixedExpenseRepository.findByMember_IdAndStatus(member.getId(), FixedExpenseStatus.ACTIVE).isEmpty()) {
            registerFromCandidate(member, "Claude 구독", new BigDecimal("22"), "USD", (short) 10);
            registerFromCandidate(member, "쿠팡와우 멤버십", new BigDecimal("7890"), "KRW", (short) 22);
            log.info("데모 고정지출 2건 시드 완료(member_id={})", member.getId());
        }

        int directSeedCount = 0;
        directSeedCount += registerDirectIfMissing(member, "월세", "430000", (short) 1);
        directSeedCount += registerDirectIfMissing(member, "관리비", "90000", (short) 10);
        directSeedCount += registerDirectIfMissing(member, "보험료", "120000", (short) 15);
        directSeedCount += registerDirectIfMissing(member, "휴대폰 요금", "59000", (short) 25);
        if (directSeedCount > 0) {
            log.info("데모 생활 고정지출 {}건 시드 완료(member_id={})", directSeedCount, member.getId());
        }

        // generateForMemberIfMissing 자체가 주기(yearMonth) 단위 존재 확인으로 멱등이라, 이미 위 등록을
        // 건너뛴(재기동) 경우에도 안전하게 매번 호출해 아직 카드가 없는 주기를 채워 넣는다.
        fixedExpenseCoachingCardService.generateForMemberIfMissing(member.getId(), resolveYearMonth(member));
        log.info("데모 고정지출 AI 코멘트(코칭 카드) 시드 완료(member_id={})", member.getId());
    }

    private void registerFromCandidate(Member member, String canonicalServiceName, BigDecimal expectedAmount,
                                        String expectedCurrency, short expectedPayDay) {
        MerchantAlias merchantAlias = merchantAliasRepository
                .findByCanonicalServiceNameNormalized(canonicalServiceName)
                .orElseThrow(() -> new IllegalStateException(canonicalServiceName + " 가맹점 별칭이 시드되지 않았습니다."));
        List<RecurringPaymentGroup> groups = recurringPaymentGroupRepository
                .findByMember_IdAndMerchantAlias_Id(member.getId(), merchantAlias.getId());
        RecurringPaymentGroup group = groups.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException(canonicalServiceName + " 반복결제 그룹이 탐지되지 않았습니다."));
        FixedExpenseCandidate candidate = fixedExpenseCandidateRepository.findByRecurringPaymentGroup_Id(group.getId())
                .orElseThrow(() -> new IllegalStateException(canonicalServiceName + " 고정지출 후보가 탐지되지 않았습니다."));

        FixedExpenseCandidateRegisterRequest request = new FixedExpenseCandidateRegisterRequest(
                canonicalServiceName, null, null, null, expectedAmount, expectedCurrency, expectedPayDay);
        fixedExpenseRegistrationService.registerFromCandidate(member.getId(), candidate.getId(), request);
    }

    private int registerDirectIfMissing(Member member, String name, String amount, short expectedPayDay) {
        if (fixedExpenseRepository.existsByMember_IdAndName(member.getId(), name)) {
            return 0;
        }
        Category category = categoryRepository.findByCode(CategoryCode.ETC.name())
                .orElseThrow(() -> new IllegalStateException("기타 카테고리가 시드되지 않았습니다."));
        FixedExpenseDirectRegisterRequest request = new FixedExpenseDirectRegisterRequest(
                name, category.getId(), FixedExpensePaymentMethod.TRANSFER, null, null,
                new BigDecimal(amount), "KRW", expectedPayDay);
        fixedExpenseRegistrationService.registerDirect(member.getId(), request);
        return 1;
    }

    /** {@link FixedExpenseCoachingCardService#runNightlyBatch}와 동일한 주기 라벨 계산이다. */
    private String resolveYearMonth(Member member) {
        if (member.getPayday() == null) {
            return FixedExpenseCyclePolicy.currentYearMonth(null);
        }
        LocalDate today = LocalDate.now(FixedExpenseCyclePolicy.ZONE_SEOUL);
        return budgetCycleService.resolveCycleForDate(member, today).yearMonth();
    }
}
