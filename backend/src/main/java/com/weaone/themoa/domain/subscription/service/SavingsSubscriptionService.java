package com.weaone.themoa.domain.subscription.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.bookmark.repository.BookmarkSavingsProductRepository;
import com.weaone.themoa.domain.financialsearch.service.BankNameFormatter;
import com.weaone.themoa.domain.financialsearch.service.BankUrlResolver;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.recommend.entity.SavingsProduct;
import com.weaone.themoa.domain.recommend.entity.SavingsProductOption;
import com.weaone.themoa.domain.recommend.service.MaturityCalculator;
import com.weaone.themoa.domain.subscription.dto.request.SubscriptionCreateRequest;
import com.weaone.themoa.domain.subscription.dto.request.SubscriptionUpdateRequest;
import com.weaone.themoa.domain.subscription.dto.response.SubscriptionDraftResponse;
import com.weaone.themoa.domain.subscription.dto.response.SubscriptionResponse;
import com.weaone.themoa.domain.subscription.entity.SavingsSubscription;
import com.weaone.themoa.domain.subscription.entity.SavingsSubscriptionCondition;
import com.weaone.themoa.domain.subscription.repository.SavingsSubscriptionConditionRepository;
import com.weaone.themoa.domain.subscription.repository.SavingsSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 사용자가 가입한 예·적금 등록·조회.
 *
 * <p>적용금리는 우대조건 텍스트에서 자동 계산하지 않고 사용자가 확정한 값을 저장한다(데이터에 조건별
 * 가산폭이 정확히 있지 않아서다). 대신 등록 화면 초안에서 우대조건을 정규식으로 쪼개 제공해, 사용자가
 * 처음부터 다 입력하지 않아도 되게 한다.
 *
 * <p>만기 예상금액은 {@link MaturityCalculator}(추천 기능이 쓰는 것과 동일)로 계산한다.
 */
@Service
public class SavingsSubscriptionService {

    /** finlife 금리유형코드: M이면 복리, 그 외(S)는 단리. */
    private static final String COMPOUND_RATE_TYPE_CODE = "M";

    private final SavingsSubscriptionRepository subscriptionRepository;
    private final SavingsSubscriptionConditionRepository conditionRepository;
    private final BookmarkSavingsProductRepository savingsProductRepository;
    private final MemberRepository memberRepository;
    private final PreferentialConditionCacheService conditionCacheService;
    private final BankUrlResolver bankUrlResolver;
    private final BankNameFormatter bankNameFormatter;

    public SavingsSubscriptionService(SavingsSubscriptionRepository subscriptionRepository,
                                      SavingsSubscriptionConditionRepository conditionRepository,
                                      BookmarkSavingsProductRepository savingsProductRepository,
                                      MemberRepository memberRepository,
                                      PreferentialConditionCacheService conditionCacheService,
                                      BankUrlResolver bankUrlResolver,
                                      BankNameFormatter bankNameFormatter) {
        this.subscriptionRepository = subscriptionRepository;
        this.conditionRepository = conditionRepository;
        this.savingsProductRepository = savingsProductRepository;
        this.memberRepository = memberRepository;
        this.conditionCacheService = conditionCacheService;
        this.bankUrlResolver = bankUrlResolver;
        this.bankNameFormatter = bankNameFormatter;
    }

    /**
     * 상품을 골랐을 때 가입 등록 화면을 채울 초안. 우대조건은 DB 캐시에서 읽어 매번 같은 체크리스트를
     * 보장한다(캐시가 없는 신규 상품은 여기서 파싱해 저장하므로 readOnly가 아니다).
     */
    @Transactional
    public SubscriptionDraftResponse draftFromProduct(Long productId) {
        SavingsProduct product = savingsProductRepository.findAllWithOptionsByIdIn(List.of(productId)).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));

        BigDecimal baseRate = product.getOptions().stream()
                .map(SavingsProductOption::getBaseRate)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        BigDecimal maxRate = product.getOptions().stream()
                .map(SavingsProductOption::getMaxRate)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        List<Integer> termMonths = product.getOptions().stream()
                .map(SavingsProductOption::getTermMonth)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        // 파싱 결과는 DB 캐시에서 읽는다(상품당 1회 파싱해 고정 → 몇 번을 눌러도 같은 체크리스트).
        // 캐시가 없으면(배치 전 신규 상품 등) 캐시 서비스가 지금 파싱해 저장하고 그 값을 돌려준다.
        List<PreferentialConditionParser.ParsedCondition> parsed =
                conditionCacheService.getOrParse(product.getId(), product.getSpecialCondition());
        List<SubscriptionDraftResponse.ConditionDraft> conditions = parsed.stream()
                .map(item -> new SubscriptionDraftResponse.ConditionDraft(item.description(), item.ratePercent()))
                .toList();

        return new SubscriptionDraftResponse(
                product.getId(),
                product.getProductName(),
                bankNameFormatter.toDisplayName(product.getCompanyName()),
                product.getProductType() == null ? null : product.getProductType().name(),
                baseRate,
                maxRate,
                product.getJoinMethod(),
                bankUrlResolver.resolve(product.getCompanyName()),
                termMonths,
                conditions);
    }

    @Transactional
    public Long create(Long memberId, SubscriptionCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 상품 id가 있으면 상품명·회사명·복리 여부를 상품에서 채우고, 없으면(직접 등록) 요청 값을 쓴다.
        String productName = request.productName();
        String companyName = request.companyName();
        String productType = request.productType();
        boolean compound = request.compound();
        BigDecimal appliedRate = request.appliedRate();

        if (request.productId() != null) {
            SavingsProduct product = savingsProductRepository
                    .findAllWithOptionsByIdIn(List.of(request.productId())).stream()
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
            productName = product.getProductName();
            companyName = product.getCompanyName();
            productType = product.getProductType() == null ? null : product.getProductType().name();
            compound = isCompound(product, request.termMonth());
            // 우대조건 파싱이 부정확하면 적용금리가 상품 최고금리를 넘을 수 있다(예: 상한 안내문이 조건으로
            // 오인되어 합산됨). 최고금리를 절대 넘지 못하도록 서버에서 강제로 깎는다 — 화면 경고만으로는
            // 잘못된 값이 저장되는 걸 막지 못한다.
            appliedRate = capToMaxRate(appliedRate, product);
        }
        if (!StringUtils.hasText(productName) || !StringUtils.hasText(companyName)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        SavingsSubscription subscription = SavingsSubscription.create(
                member, request.productId(), productName, companyName, productType,
                request.monthlyAmount(), appliedRate, request.termMonth(), compound,
                request.startDate(), LocalDateTime.now());

        if (request.conditions() != null) {
            for (SubscriptionCreateRequest.ConditionInput input : request.conditions()) {
                if (!StringUtils.hasText(input.description())) {
                    continue;
                }
                subscription.addCondition(SavingsSubscriptionCondition.of(
                        input.description().trim(), input.rateBonus(), input.met()));
            }
        }
        return subscriptionRepository.save(subscription).getId();
    }

    /** 대시보드 목록(만기 예상금액 포함). */
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> findAll(Long memberId) {
        return subscriptionRepository.findAllWithConditionsByMemberId(memberId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** 우대조건 충족 여부 토글(본인 것만). */
    @Transactional
    public void updateConditionMet(Long memberId, Long conditionId, boolean met) {
        SavingsSubscriptionCondition condition = conditionRepository
                .findByIdAndSubscription_Member_Id(conditionId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
        condition.updateMet(met);
    }

    /**
     * 가입 정보 수정(본인 것만). 원본 상품이 있으면 복리 여부는 바뀐 기간에 맞춰 다시 판단하고,
     * 적용금리는 상품 최고금리를 넘지 못하도록 서버에서 깎는다(등록과 같은 규칙).
     */
    @Transactional
    public void update(Long memberId, Long subscriptionId, SubscriptionUpdateRequest request) {
        SavingsSubscription subscription = subscriptionRepository
                .findByIdAndMember_Id(subscriptionId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));

        BigDecimal appliedRate = request.appliedRate();
        boolean compound = subscription.isCompound();
        if (subscription.getProductId() != null) {
            SavingsProduct product = savingsProductRepository
                    .findAllWithOptionsByIdIn(List.of(subscription.getProductId())).stream()
                    .findFirst()
                    .orElse(null);
            if (product != null) {
                compound = isCompound(product, request.termMonth());
                appliedRate = capToMaxRate(appliedRate, product);
            }
        }
        subscription.update(request.monthlyAmount(), appliedRate, request.termMonth(),
                compound, request.startDate());
    }

    @Transactional
    public void delete(Long memberId, Long subscriptionId) {
        SavingsSubscription subscription = subscriptionRepository
                .findByIdAndMember_Id(subscriptionId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
        subscriptionRepository.delete(subscription);
    }

    /** 적용금리가 상품 최고우대금리를 넘으면 최고금리로 깎는다. 최고금리 정보가 없으면 그대로 둔다. */
    private BigDecimal capToMaxRate(BigDecimal appliedRate, SavingsProduct product) {
        if (appliedRate == null) {
            return null;
        }
        BigDecimal maxRate = product.getOptions().stream()
                .map(SavingsProductOption::getMaxRate)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        if (maxRate != null && appliedRate.compareTo(maxRate) > 0) {
            return maxRate;
        }
        return appliedRate;
    }

    private boolean isCompound(SavingsProduct product, int termMonth) {
        return product.getOptions().stream()
                .filter(option -> Objects.equals(option.getTermMonth(), termMonth))
                .findFirst()
                .map(option -> COMPOUND_RATE_TYPE_CODE.equalsIgnoreCase(option.getRateTypeCode()))
                .orElse(false);
    }

    private SubscriptionResponse toResponse(SavingsSubscription subscription) {
        long totalPrincipal = subscription.getMonthlyAmount() * subscription.getTermMonth();
        long expectedMaturity = MaturityCalculator.installmentMaturity(
                subscription.getMonthlyAmount(), subscription.getAppliedRate(),
                subscription.getTermMonth(), subscription.isCompound());

        // 현재 시점: 가입일부터 오늘까지 경과한 개월(만기 초과 시 만기로 고정)로 원금·평가액을 계산한다.
        // 만기 계산과 같은 공식에 개월수만 경과분으로 바꿔 넣어, 만기 예상치와 일관되게 근사한다.
        int elapsed = elapsedMonths(subscription);
        long currentPrincipal = subscription.getMonthlyAmount() * elapsed;
        long currentValue = MaturityCalculator.installmentMaturity(
                subscription.getMonthlyAmount(), subscription.getAppliedRate(),
                elapsed, subscription.isCompound());

        List<SubscriptionResponse.ConditionResponse> conditions = subscription.getConditions().stream()
                .map(c -> new SubscriptionResponse.ConditionResponse(
                        c.getId(), c.getDescription(), c.getRateBonus(), c.isMet()))
                .toList();
        int unmet = (int) conditions.stream().filter(c -> !c.met()).count();

        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getProductId(),
                subscription.getProductName(),
                subscription.getCompanyName(),
                subscription.getProductType(),
                subscription.getMonthlyAmount(),
                subscription.getAppliedRate(),
                subscription.getTermMonth(),
                subscription.isCompound(),
                subscription.getStartDate(),
                subscription.getMaturityDate(),
                totalPrincipal,
                currentPrincipal,
                currentValue,
                expectedMaturity,
                unmet,
                conditions);
    }

    /** 가입일 포함 지금까지 납입한 회차 수. 가입 당일 첫 납입을 1회차로 본다. 만기(termMonth) 초과면 termMonth로 고정. */
    private int elapsedMonths(SavingsSubscription subscription) {
        if (subscription.getStartDate() == null) {
            return 0;
        }
        // 미래 가입일이면 아직 납입 전이라 0.
        if (subscription.getStartDate().isAfter(LocalDate.now())) {
            return 0;
        }
        long months = ChronoUnit.MONTHS.between(subscription.getStartDate(), LocalDate.now());
        return (int) Math.min(months + 1, subscription.getTermMonth());
    }
}
