package com.weaone.themoa.domain.fixedexpense.service;

import com.weaone.themoa.domain.budget.service.BudgetCycleService;
import com.weaone.themoa.domain.cardconnection.entity.CardConnection;
import com.weaone.themoa.domain.cardconnection.entity.ConnectionStatus;
import com.weaone.themoa.domain.cardconnection.repository.CardConnectionRepository;
import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.category.repository.CategoryRepository;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseCandidate;
import com.weaone.themoa.domain.fixedexpense.entity.RecurringPaymentGroup;
import com.weaone.themoa.domain.fixedexpense.entity.RecurringPaymentGroupTransaction;
import com.weaone.themoa.domain.fixedexpense.entity.RecurringPaymentGroupTransactionId;
import com.weaone.themoa.domain.fixedexpense.entity.UserMerchantPreferenceType;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseCandidateRepository;
import com.weaone.themoa.domain.fixedexpense.repository.RecurringPaymentGroupRepository;
import com.weaone.themoa.domain.fixedexpense.repository.RecurringPaymentGroupTransactionRepository;
import com.weaone.themoa.domain.fixedexpense.repository.UserMerchantPreferenceRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.merchant.entity.Merchant;
import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 반복결제 탐지 새벽 배치(fixedExpense.md §2·§3). 새벽 배치 1회에서만 실행하고 온디맨드에서는 돌지 않는다
 * — 재수집이 아니라 재스캔이라 다음 배치가 놓친 거래까지 포함해 다시 판정한다.
 *
 * <p>이름형은 alias 레벨로 그룹핑한다. biller(Apple 등) 경유 결제는 이름으로 alias가 안 붙으므로
 * merchant 단위로 먼저 묶은 뒤 금액으로 사전 클러스터링해 그룹핑한다(merchant.md §5-D-3,
 * troubleshooting/billerProblem.md).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FixedExpenseDetectionService {

    private static final long MIN_REPEAT_COUNT = 3;
    private static final long INACTIVITY_LIMIT_DAYS = 30;

    private final CardConnectionRepository cardConnectionRepository;
    private final MemberRepository memberRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final RecurringPaymentGroupRepository recurringPaymentGroupRepository;
    private final RecurringPaymentGroupTransactionRepository recurringPaymentGroupTransactionRepository;
    private final FixedExpenseCandidateRepository fixedExpenseCandidateRepository;
    private final UserMerchantPreferenceRepository userMerchantPreferenceRepository;
    private final CategoryRepository categoryRepository;
    private final RecurringPatternDetector recurringPatternDetector;
    private final AmountClusterer amountClusterer;
    private final BudgetCycleService budgetCycleService;

    /**
     * 카드거래 수집 새벽 배치(03:00) 이후에 돌도록 30분 뒤로 잡는다.
     *
     * <p>{@code @Transactional}이 필요하다 — {@link #currentYearMonth}가 {@code memberRepository.getReferenceById}로
     * 만든 프록시를 별도 빈({@link BudgetCycleService})의 트랜잭션에 넘기므로, 이 메서드 자체가 트랜잭션을
     * 열어 두지 않으면 조회·사용 트랜잭션이 갈려 {@code LazyInitializationException}이 난다
     * (troubleshooting/lazyinitialization.md 사례 2). 카드 동기화 배치와 달리 이 배치는 외부 API 호출이
     * 없는 순수 DB 작업이라 전체를 하나의 트랜잭션으로 묶어도 커넥션을 오래 붙잡는 부담이 없다.
     */
    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Seoul")
    @Transactional
    public void runNightlyDetection() {
        LocalDateTime activeSince = LocalDateTime.now(FixedExpenseCyclePolicy.ZONE_SEOUL).minusDays(INACTIVITY_LIMIT_DAYS);
        LocalDate today = LocalDate.now(FixedExpenseCyclePolicy.ZONE_SEOUL);
        List<CardConnection> connections = cardConnectionRepository
                .findEligibleForNightlyBatch(ConnectionStatus.ACTIVE, activeSince);
        Map<Long, Member> membersById = connections.stream()
                .map(CardConnection::getMember)
                .collect(Collectors.toMap(Member::getId, member -> member, (a, b) -> a, LinkedHashMap::new));
        for (Member member : membersById.values()) {
            detectForMember(member.getId(), currentYearMonth(member.getId(), today));
        }
    }

    /** 로그인 회원 본인 범위 즉시 실행(F-04 테스트용 트리거)용 진입점 — payday를 직접 조회해 배치와 같은 라벨을 계산한다. */
    @Transactional
    public void detectForMember(Long memberId) {
        detectForMember(memberId, currentYearMonth(memberId, LocalDate.now(FixedExpenseCyclePolicy.ZONE_SEOUL)));
    }

    /** 급여일 변경 예약을 먼저 승격한 뒤(payday.md §급여일 변경) 최신 payday·이력 기준으로 라벨을 계산한다. */
    private String currentYearMonth(Long memberId, LocalDate today) {
        budgetCycleService.ensurePaydayPromoted(memberId, today);
        Member member = memberRepository.getReferenceById(memberId);
        return budgetCycleService.resolveCycleForDate(member, today).yearMonth();
    }

    @Transactional
    public void detectForMember(Long memberId, String currentYearMonth) {
        List<CardTransactionRepository.AliasGroupCount> aliasGroups = cardTransactionRepository
                .findAliasGroupCandidates(memberId, TransactionStatus.CANCELED, MIN_REPEAT_COUNT);
        for (CardTransactionRepository.AliasGroupCount group : aliasGroups) {
            try {
                processAliasGroup(memberId, group.getMerchantAliasId(), currentYearMonth);
            } catch (RuntimeException e) {
                log.warn("고정지출 탐지 1건 처리 실패, 다음 그룹으로 계속 진행합니다. memberId={}, merchantAliasId={}",
                        memberId, group.getMerchantAliasId(), e);
            }
        }

        List<CardTransactionRepository.MerchantGroupCount> billerGroups = cardTransactionRepository
                .findBillerMerchantGroupCandidates(memberId, TransactionStatus.CANCELED, MIN_REPEAT_COUNT);
        for (CardTransactionRepository.MerchantGroupCount group : billerGroups) {
            try {
                processBillerMerchant(memberId, group.getMerchantId(), currentYearMonth);
            } catch (RuntimeException e) {
                log.warn("biller 고정지출 탐지 1건 처리 실패, 다음 merchant로 계속 진행합니다. memberId={}, merchantId={}",
                        memberId, group.getMerchantId(), e);
            }
        }
    }

    /**
     * alias 하나에 서로 다른 구독이 공존할 수 있어(예: 같은 서비스를 계정 두 개로 구독) {@link RecurringPatternDetector#detectAll}로
     * 여러 패턴을 한 번에 뽑는다. 금액이 아니라 이미 링크된 증거 거래와의 겹침으로 기존 그룹을 찾으므로
     * 값이 비슷한 별개 구독끼리도 안 섞인다(아래 {@link #findMatchingGroup}).
     */
    private void processAliasGroup(Long memberId, Long merchantAliasId, String currentYearMonth) {
        List<CardTransaction> transactions = cardTransactionRepository
                .findByMember_IdAndMerchantAlias_IdAndStatusNotOrderByUsedDateAsc(
                        memberId, merchantAliasId, TransactionStatus.CANCELED);
        List<DetectedPattern> patterns = recurringPatternDetector.detectAll(transactions);
        if (patterns.isEmpty()) {
            return;
        }

        List<RecurringPaymentGroup> existingGroups = recurringPaymentGroupRepository
                .findByMember_IdAndMerchantAlias_Id(memberId, merchantAliasId);
        for (DetectedPattern pattern : patterns) {
            try {
                processAliasPattern(memberId, existingGroups, pattern, currentYearMonth);
            } catch (RuntimeException e) {
                log.warn("고정지출 탐지 패턴 처리 실패, 다음 패턴으로 계속 진행합니다. memberId={}, merchantAliasId={}",
                        memberId, merchantAliasId, e);
            }
        }
    }

    private void processAliasPattern(Long memberId, List<RecurringPaymentGroup> existingGroups,
                                      DetectedPattern pattern, String currentYearMonth) {
        CardTransaction sample = pattern.transactions().get(0);
        Member member = sample.getMember();
        MerchantAlias merchantAlias = sample.getMerchantAlias();

        RecurringPaymentGroup group = findMatchingGroup(existingGroups, pattern)
                .map(existing -> {
                    existing.updateStats((short) pattern.transactions().size(), pattern.avgAmount(),
                            pattern.amountVariancePct(), pattern.avgPayDay(), pattern.payDayVariance(),
                            pattern.lastDetectedAt());
                    return existing;
                })
                .orElseGet(() -> {
                    RecurringPaymentGroup created = recurringPaymentGroupRepository.save(RecurringPaymentGroup.detect(
                            member, merchantAlias, (short) pattern.transactions().size(), pattern.avgAmount(),
                            pattern.amountVariancePct(), pattern.avgPayDay(), pattern.payDayVariance(),
                            pattern.lastDetectedAt()));
                    existingGroups.add(created);
                    return created;
                });

        linkEvidenceTransactions(group.getId(), pattern);
        if (isGroupSuppressed(memberId, group.getId())) {
            return;
        }
        Category recommendedCategory = merchantAlias.getDefaultCategory() != null
                ? merchantAlias.getDefaultCategory()
                : etcCategory();
        String message = "매달 " + pattern.avgAmount().toBigInteger() + "원 나가는 "
                + merchantAlias.getCanonicalServiceName() + "(매월 " + pattern.avgPayDay() + "일), 등록할까요?";
        upsertCandidate(member, group, recommendedCategory, message, pattern, currentYearMonth);
    }

    /**
     * biller 경유 그룹핑(merchant.md §5-D-3): 이름으로 alias가 안 붙으므로 merchant 단위로 모은 거래를
     * 먼저 금액으로 사전 클러스터링한 뒤, 버킷마다 {@link RecurringPatternDetector#detectAll}을 호출한다.
     * 한 merchant(예: Apple)에서 서로 다른 금액대의 구독이 섞여 있으면 버킷 수만큼, 같은 버킷 안에 결제일이
     * 다른 별개 구독이 섞여 있으면 패턴 수만큼 그룹 후보가 나온다.
     */
    private void processBillerMerchant(Long memberId, Long merchantId, String currentYearMonth) {
        List<CardTransaction> transactions = cardTransactionRepository
                .findByMember_IdAndMerchant_IdAndMerchantAliasIsNullAndStatusNotOrderByAmountAsc(
                        memberId, merchantId, TransactionStatus.CANCELED);
        List<RecurringPaymentGroup> existingGroups = recurringPaymentGroupRepository
                .findByMember_IdAndBillerMerchant_Id(memberId, merchantId);
        for (List<CardTransaction> bucket : amountClusterer.cluster(transactions)) {
            bucket.sort(Comparator.comparing(CardTransaction::getUsedDate));
            for (DetectedPattern pattern : recurringPatternDetector.detectAll(bucket)) {
                try {
                    processBillerPattern(memberId, merchantId, existingGroups, pattern, currentYearMonth);
                } catch (RuntimeException e) {
                    log.warn("biller 고정지출 탐지 패턴 처리 실패, 다음 패턴으로 계속 진행합니다. memberId={}, merchantId={}",
                            memberId, merchantId, e);
                }
            }
        }
    }

    private void processBillerPattern(Long memberId, Long merchantId, List<RecurringPaymentGroup> existingGroups,
                                       DetectedPattern pattern, String currentYearMonth) {
        CardTransaction sample = pattern.transactions().get(0);
        Member member = sample.getMember();
        Merchant billerMerchant = sample.getMerchant();

        RecurringPaymentGroup group = findMatchingGroup(existingGroups, pattern)
                .map(existing -> {
                    existing.updateStats((short) pattern.transactions().size(), pattern.avgAmount(),
                            pattern.amountVariancePct(), pattern.avgPayDay(), pattern.payDayVariance(),
                            pattern.lastDetectedAt());
                    return existing;
                })
                .orElseGet(() -> {
                    RecurringPaymentGroup created = recurringPaymentGroupRepository.save(RecurringPaymentGroup.detectBiller(
                            member, billerMerchant, (short) pattern.transactions().size(), pattern.avgAmount(),
                            pattern.amountVariancePct(), pattern.avgPayDay(), pattern.payDayVariance(),
                            pattern.lastDetectedAt()));
                    existingGroups.add(created);
                    return created;
                });

        linkEvidenceTransactions(group.getId(), pattern);
        if (isGroupSuppressed(memberId, group.getId())) {
            return;
        }
        String message = pattern.avgAmount().toBigInteger() + "원씩 " + billerMerchant.getMerchantNameRaw()
                + "로 매월 " + pattern.avgPayDay() + "일쯤 나가는 결제가 있어요. 어떤 서비스인지 이름을 지어 등록할까요?";
        upsertCandidate(member, group, etcCategory(), message, pattern, currentYearMonth);
    }

    /**
     * find-or-create의 신원 판단 기준(fixedExpense.md §2): 금액이 아니라 이 그룹에 이미 링크된 증거
     * 거래와의 겹침으로 "같은 구독의 재탐지인지"를 가른다. 같은 alias(또는 biller merchant) 안에 값이
     * 비슷한 별개 구독이 있으면 금액만으론 못 갈라서, 결제일 간격으로 나뉜 패턴별 실제 거래로 판단한다.
     */
    private Optional<RecurringPaymentGroup> findMatchingGroup(List<RecurringPaymentGroup> existingGroups,
                                                                DetectedPattern pattern) {
        if (existingGroups.isEmpty()) {
            return Optional.empty();
        }
        List<Long> patternTransactionIds = pattern.transactions().stream().map(CardTransaction::getId).toList();
        return existingGroups.stream()
                .filter(existing -> recurringPaymentGroupTransactionRepository
                        .existsOverlap(existing.getId(), patternTransactionIds))
                .findFirst();
    }

    /** 거절·습관분류는 그룹 단위로 억제한다 — alias·biller merchant 단위로 걸면 공존하는 별개 구독까지 함께 막힌다. */
    private boolean isGroupSuppressed(Long memberId, Long recurringGroupId) {
        return userMerchantPreferenceRepository.existsByMember_IdAndRecurringPaymentGroup_IdAndPreferenceType(
                memberId, recurringGroupId, UserMerchantPreferenceType.DO_NOT_RECOMMEND)
                || userMerchantPreferenceRepository.existsByMember_IdAndRecurringPaymentGroup_IdAndPreferenceType(
                memberId, recurringGroupId, UserMerchantPreferenceType.RECLASSIFY_HABIT);
    }

    private void linkEvidenceTransactions(Long groupId, DetectedPattern pattern) {
        for (CardTransaction tx : pattern.transactions()) {
            RecurringPaymentGroupTransactionId id = new RecurringPaymentGroupTransactionId(groupId, tx.getId());
            if (!recurringPaymentGroupTransactionRepository.existsById(id)) {
                recurringPaymentGroupTransactionRepository.save(RecurringPaymentGroupTransaction.of(groupId, tx.getId()));
            }
        }
    }

    /** 후보 상태머신(§3): 그룹당 후보 1행, 재추천은 기존 행 UPDATE — 신규 INSERT는 최초 탐지 1회뿐. */
    private void upsertCandidate(Member member, RecurringPaymentGroup group, Category recommendedCategory,
                                  String message, DetectedPattern pattern, String currentYearMonth) {
        // 점수 산정 공식은 문서에 별도 정의가 없어 반복 횟수를 신뢰도 점수로 그대로 사용한다.
        BigDecimal score = BigDecimal.valueOf(pattern.transactions().size());

        FixedExpenseCandidate candidate = fixedExpenseCandidateRepository
                .findByRecurringPaymentGroup_Id(group.getId()).orElse(null);
        if (candidate == null) {
            fixedExpenseCandidateRepository.save(
                    FixedExpenseCandidate.create(member, group, recommendedCategory, score, message));
            return;
        }

        switch (candidate.getStatus()) {
            case PENDING -> candidate.refresh(recommendedCategory, score, message);
            case EXCLUDED_THIS_MONTH -> {
                if (candidate.isSnoozeExpired(currentYearMonth)) {
                    candidate.reopen();
                    candidate.refresh(recommendedCategory, score, message);
                }
            }
            case REGISTERED, DO_NOT_RECOMMEND, CLASSIFIED_HABIT -> {
                // 응답 완료 — 재추천하지 않는다.
            }
        }
    }

    private Category etcCategory() {
        return categoryRepository.findByCode(CategoryCode.ETC.name())
                .orElseThrow(() -> new IllegalStateException("ETC 카테고리 시드가 없습니다."));
    }
}
