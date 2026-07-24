package com.weaone.themoa.domain.fixedexpense.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.category.repository.CategoryRepository;
import com.weaone.themoa.domain.cardtransaction.service.ExchangeRateUnavailableException;
import com.weaone.themoa.domain.fixedexpense.dto.request.FixedExpenseCandidateRegisterRequest;
import com.weaone.themoa.domain.fixedexpense.dto.request.FixedExpenseDirectRegisterRequest;
import com.weaone.themoa.domain.fixedexpense.dto.request.FixedExpenseUpdateRequest;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseCandidate;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseCandidateStatus;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpensePaymentMethod;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseStatus;
import com.weaone.themoa.domain.fixedexpense.entity.RecurringPaymentGroup;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseCandidateRepository;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.merchant.entity.Merchant;
import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasRepository;
import com.weaone.themoa.domain.merchant.service.MerchantIdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 고정지출 등록·조회·수정·해지(fixedExpense.md §4, F-01·F-03·F-04). 두 등록 경로 모두 이 서비스를 거친다.
 *
 * <p>biller(Apple 등) 경유 구독은 {@link FixedExpenseDetectionService}가 탐지한 biller 후보를
 * 승인할 때 이 서비스가 그룹의 {@code billerMerchant}를 그대로 이어받아 {@code biller_merchant_id}를
 * 채운다(troubleshooting/billerProblem.md). 직접 등록(경로 B)으로 만든 CARD형 구독은 등록 시점엔
 * 아직 결제내역이 없어 이 값을 알 수 없으므로 여전히 NULL이다 — 이 경우는 F-05 미납 확인 시점에
 * {@code FixedExpenseMatchingService}가 확인된 거래의 merchant로 소급 채운다.
 */
@Service
@RequiredArgsConstructor
public class FixedExpenseRegistrationService {

    private static final String CURRENCY_KRW = "KRW";

    private final FixedExpenseRepository fixedExpenseRepository;
    private final FixedExpenseCandidateRepository fixedExpenseCandidateRepository;
    private final CategoryRepository categoryRepository;
    private final MerchantAliasRepository merchantAliasRepository;
    private final MerchantIdentityService merchantIdentityService;
    private final MemberRepository memberRepository;
    private final FixedExpenseKrwConverter krwConverter;

    @Transactional
    public FixedExpense registerFromCandidate(Long memberId, Long candidateId,
                                               FixedExpenseCandidateRegisterRequest request) {
        FixedExpenseCandidate candidate = fixedExpenseCandidateRepository.findByIdAndMember_Id(candidateId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FIXED_EXPENSE_CANDIDATE_NOT_FOUND));
        if (candidate.getStatus() != FixedExpenseCandidateStatus.PENDING) {
            throw new BusinessException(ErrorCode.FIXED_EXPENSE_CANDIDATE_NOT_PENDING);
        }

        Member member = memberRepository.getReferenceById(memberId);
        Category category = request.categoryId() != null
                ? getCategory(request.categoryId())
                : candidate.getRecommendedCategory();
        RecurringPaymentGroup group = candidate.getRecurringPaymentGroup();
        MerchantAlias merchantAlias;
        Merchant billerMerchant;
        if (group.isBillerGroup()) {
            // biller 후보는 그룹에 alias가 없어 승인 시점에 사용자가 이름을 짓는다(troubleshooting/billerProblem.md).
            merchantAlias = resolveOrCreateAlias(memberId, request.merchantAliasId(), request.newMerchantAliasName(),
                    request.categoryId());
            billerMerchant = group.getBillerMerchant();
        } else {
            merchantAlias = group.getMerchantAlias();
            billerMerchant = null;
        }
        String currency = resolveCurrency(request.expectedCurrency());
        ConvertedKrwAmount converted = convertToKrw(request.expectedAmount(), currency);

        FixedExpense fixedExpense = fixedExpenseRepository.save(FixedExpense.fromCandidate(member, candidate, request.name(),
                category, merchantAlias, billerMerchant, request.expectedPayDay(), request.expectedAmount(), currency,
                converted.krwAmount(), converted.convertedDate(), converted.exchangeRate()));
        candidate.register();
        return fixedExpense;
    }

    @Transactional
    public FixedExpense registerDirect(Long memberId, FixedExpenseDirectRegisterRequest request) {
        Member member = memberRepository.getReferenceById(memberId);
        Category category = getCategory(request.categoryId());
        MerchantAlias merchantAlias = resolveMerchantAliasForDirectRegister(memberId, request);
        String currency = resolveCurrency(request.expectedCurrency());
        ConvertedKrwAmount converted = convertToKrw(request.expectedAmount(), currency);

        return fixedExpenseRepository.save(FixedExpense.registerDirect(member, request.name(), category,
                merchantAlias, request.paymentMethod(), request.expectedPayDay(), request.expectedAmount(), currency,
                converted.krwAmount(), converted.convertedDate(), converted.exchangeRate()));
    }

    @Transactional(readOnly = true)
    public List<FixedExpense> list(Long memberId) {
        return fixedExpenseRepository.findByMember_IdAndStatus(memberId, FixedExpenseStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public FixedExpense get(Long memberId, Long fixedExpenseId) {
        return getOwned(memberId, fixedExpenseId);
    }

    @Transactional
    public void update(Long memberId, Long fixedExpenseId, FixedExpenseUpdateRequest request) {
        FixedExpense fixedExpense = getOwned(memberId, fixedExpenseId);
        String currency = resolveCurrency(request.expectedCurrency());
        ConvertedKrwAmount converted = convertToKrw(request.expectedAmount(), currency);
        fixedExpense.updateExpected(request.expectedAmount(), currency, converted.krwAmount(),
                converted.convertedDate(), converted.exchangeRate(), request.expectedPayDay());
    }

    /** 해지 시 후보를 함께 되돌린다 — REGISTERED로 남으면 재탐지에서 영구히 제외되기 때문(fixedExpenseCandidateReopen 이슈). */
    @Transactional
    public void cancel(Long memberId, Long fixedExpenseId) {
        FixedExpense fixedExpense = getOwned(memberId, fixedExpenseId);
        fixedExpense.cancel();
        FixedExpenseCandidate candidate = fixedExpense.getCandidate();
        if (candidate != null) {
            candidate.reopen();
        }
    }

    /**
     * 경로 C: 예·적금 가입 시 자동 등록(fixedExpense.md §7). subscription 도메인이 발행한 이벤트를
     * 구독해 호출되므로, 이벤트 재전달로 두 번 불려도 같은 결과가 되도록 이미 연동된 행이 있으면
     * 조용히 건너뛴다(멱등).
     */
    @Transactional
    public void registerFromSavingsSubscription(Long memberId, Long savingsSubscriptionId, String productName,
                                                 Long monthlyAmount) {
        if (fixedExpenseRepository.findBySavingsSubscriptionId(savingsSubscriptionId).isPresent()) {
            return;
        }
        Member member = memberRepository.getReferenceById(memberId);
        Category category = categoryRepository.findByCode(CategoryCode.SAVING.name())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        fixedExpenseRepository.save(FixedExpense.fromSavingsSubscription(member, savingsSubscriptionId, productName,
                category, BigDecimal.valueOf(monthlyAmount)));
    }

    /** 경로 C 연동 건의 월납입액 변경 반영. 연동된 고정지출이 없으면(이미 해지 등) 조용히 넘어간다. */
    @Transactional
    public void updateAmountFromSavingsSubscription(Long savingsSubscriptionId, Long monthlyAmount) {
        fixedExpenseRepository.findBySavingsSubscriptionId(savingsSubscriptionId)
                .ifPresent(fixedExpense -> fixedExpense.updateAmountFromSavingsSubscription(BigDecimal.valueOf(monthlyAmount)));
    }

    /** 경로 C 연동 건 해지. 가입 기록 삭제에 맞춰 고정지출도 다음 주기부터 예산 차감에서 뺀다. */
    @Transactional
    public void cancelFromSavingsSubscription(Long savingsSubscriptionId) {
        fixedExpenseRepository.findBySavingsSubscriptionId(savingsSubscriptionId)
                .ifPresent(FixedExpense::cancel);
    }

    private MerchantAlias resolveMerchantAliasForDirectRegister(Long memberId, FixedExpenseDirectRegisterRequest request) {
        if (request.paymentMethod() != FixedExpensePaymentMethod.CARD) {
            return null;
        }
        return resolveOrCreateAlias(memberId, request.merchantAliasId(), request.newMerchantAliasName(), request.categoryId());
    }

    /** 기존 alias를 고르거나(merchantAliasId) 새로 만든다(newMerchantAliasName). 직접등록·biller 후보 승인 공용. */
    private MerchantAlias resolveOrCreateAlias(Long memberId, Long merchantAliasId, String newMerchantAliasName,
                                                Long categoryId) {
        if (merchantAliasId != null) {
            return merchantAliasRepository.findById(merchantAliasId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_ALIAS_NOT_FOUND));
        }
        if (StringUtils.hasText(newMerchantAliasName)) {
            return merchantIdentityService.registerAlias(memberId, newMerchantAliasName, categoryId, null);
        }
        throw new BusinessException(ErrorCode.FIXED_EXPENSE_MERCHANT_ALIAS_REQUIRED);
    }

    private ConvertedKrwAmount convertToKrw(BigDecimal amount, String currency) {
        LocalDate today = LocalDate.now(FixedExpenseCyclePolicy.ZONE_SEOUL);
        try {
            return krwConverter.convert(amount, currency, today);
        } catch (ExchangeRateUnavailableException e) {
            // 등록 경로는 원화 스냅샷이 NOT NULL이라 환율을 못 구하면 등록 자체를 막는다(erd.md §5).
            throw new BusinessException(ErrorCode.FIXED_EXPENSE_EXCHANGE_RATE_UNAVAILABLE);
        }
    }

    private String resolveCurrency(String requested) {
        return StringUtils.hasText(requested) ? requested.toUpperCase() : CURRENCY_KRW;
    }

    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private FixedExpense getOwned(Long memberId, Long fixedExpenseId) {
        return fixedExpenseRepository.findByIdAndMember_Id(fixedExpenseId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FIXED_EXPENSE_NOT_FOUND));
    }
}
