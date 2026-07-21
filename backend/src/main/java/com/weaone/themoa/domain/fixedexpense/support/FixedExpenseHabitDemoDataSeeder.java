package com.weaone.themoa.domain.fixedexpense.support;

import com.weaone.themoa.domain.budget.entity.Budget;
import com.weaone.themoa.domain.budget.entity.BudgetIncomeAdjustment;
import com.weaone.themoa.domain.budget.entity.SurplusFund;
import com.weaone.themoa.domain.budget.repository.BudgetIncomeAdjustmentRepository;
import com.weaone.themoa.domain.budget.repository.BudgetRepository;
import com.weaone.themoa.domain.budget.repository.SurplusFundRepository;
import com.weaone.themoa.domain.budget.service.BudgetCyclePolicy;
import com.weaone.themoa.domain.cardconnection.entity.Card;
import com.weaone.themoa.domain.cardconnection.entity.CardConnection;
import com.weaone.themoa.domain.cardconnection.entity.CardIssuer;
import com.weaone.themoa.domain.cardconnection.repository.CardConnectionRepository;
import com.weaone.themoa.domain.cardconnection.repository.CardIssuerRepository;
import com.weaone.themoa.domain.cardconnection.repository.CardRepository;
import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import com.weaone.themoa.domain.cardtransaction.entity.ExchangeRateCache;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.cardtransaction.repository.ExchangeRateCacheRepository;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.category.repository.CategoryRepository;
import com.weaone.themoa.domain.coaching.entity.CoachingCard;
import com.weaone.themoa.domain.coaching.entity.CoachingDismiss;
import com.weaone.themoa.domain.coaching.entity.CoachingDismissType;
import com.weaone.themoa.domain.coaching.repository.CoachingCardRepository;
import com.weaone.themoa.domain.coaching.repository.CoachingDismissRepository;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseCandidate;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpensePayment;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpensePaymentMethod;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseStatus;
import com.weaone.themoa.domain.fixedexpense.entity.RecurringPaymentGroup;
import com.weaone.themoa.domain.fixedexpense.entity.RecurringPaymentGroupTransaction;
import com.weaone.themoa.domain.fixedexpense.entity.UserMerchantPreference;
import com.weaone.themoa.domain.fixedexpense.entity.UserMerchantPreferenceType;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseCandidateRepository;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpensePaymentRepository;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseRepository;
import com.weaone.themoa.domain.fixedexpense.repository.RecurringPaymentGroupRepository;
import com.weaone.themoa.domain.fixedexpense.repository.RecurringPaymentGroupTransactionRepository;
import com.weaone.themoa.domain.fixedexpense.repository.UserMerchantPreferenceRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.member.support.MemberDemoSeeder;
import com.weaone.themoa.domain.merchant.entity.Merchant;
import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
import com.weaone.themoa.domain.merchant.entity.MerchantAliasTerms;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasTermsRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantRepository;
import com.weaone.themoa.domain.notification.entity.NotificationTypeCode;
import com.weaone.themoa.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 로컬 개발용 고정지출·습관지출 코칭 종합 더미데이터 시드(fixedExpense.md, habitExpense.md, dailyBudget.md).
 * {@code solmin} 계정(카드동기화 모드) 하나에 반복결제 탐지 후보의 상태값 6가지(PENDING·REGISTERED·
 * EXCLUDED_THIS_MONTH·DO_NOT_RECOMMEND·CLASSIFIED_HABIT 전체 + biller 경유 케이스)와 직접등록 경로(카드형·
 * 계좌이체형)를 모두 채워, 이 회원 하나만으로 고정지출·습관지출 화면의 거의 모든 상태를 확인할 수 있게 한다.
 *
 * <p>{@link MemberDemoSeeder}(회원)·{@code CardIssuerSeeder}(카드사)·{@code CategorySeeder}(카테고리)·
 * {@code MerchantAliasSeeder}(전역 alias/biller)가 먼저 끝나 있어야 하므로 그보다 뒤에 돈다({@link Order}).
 * {@code Budget}은 실제로는 소비 가이드 진입 시 지연 생성되지만(§{@link BudgetCycleService}), 화면을 바로
 * 확인할 수 있도록 이 시더가 최근 4주기(완료 3 + 진행 중 1) 스냅샷을 직접 만들어 둔다.
 */
@Component
@Order(6)
@RequiredArgsConstructor
public class FixedExpenseHabitDemoDataSeeder implements ApplicationRunner {

    private static final String KRW = "KRW";

    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final CardIssuerRepository cardIssuerRepository;
    private final CardConnectionRepository cardConnectionRepository;
    private final CardRepository cardRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantAliasRepository merchantAliasRepository;
    private final MerchantAliasTermsRepository merchantAliasTermsRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final ExchangeRateCacheRepository exchangeRateCacheRepository;
    private final RecurringPaymentGroupRepository recurringPaymentGroupRepository;
    private final RecurringPaymentGroupTransactionRepository recurringPaymentGroupTransactionRepository;
    private final FixedExpenseCandidateRepository fixedExpenseCandidateRepository;
    private final FixedExpenseRepository fixedExpenseRepository;
    private final FixedExpensePaymentRepository fixedExpensePaymentRepository;
    private final UserMerchantPreferenceRepository userMerchantPreferenceRepository;
    private final BudgetRepository budgetRepository;
    private final SurplusFundRepository surplusFundRepository;
    private final BudgetIncomeAdjustmentRepository budgetIncomeAdjustmentRepository;
    private final CoachingCardRepository coachingCardRepository;
    private final CoachingDismissRepository coachingDismissRepository;
    private final NotificationService notificationService;

    private final AtomicInteger domesticApprovalSeq = new AtomicInteger(20_260_001);
    private final AtomicInteger overseasApprovalSeq = new AtomicInteger(300_001);

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (recurringPaymentGroupRepository.count() > 0) {
            return;
        }
        Optional<Member> solminOpt = memberRepository.findByEmail(MemberDemoSeeder.SOLMIN_EMAIL);
        if (solminOpt.isEmpty()) {
            return;
        }
        Member solmin = solminOpt.get();

        Map<CategoryCode, Category> categories = loadCategories();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        BudgetCyclePolicy.BudgetCycle cycle0 = BudgetCyclePolicy.cycleOf(solmin.getPayday(), today);
        BudgetCyclePolicy.BudgetCycle cycle1 =
                BudgetCyclePolicy.cycleOf(solmin.getPayday(), cycle0.cycleStartDate().minusDays(1));
        BudgetCyclePolicy.BudgetCycle cycle2 =
                BudgetCyclePolicy.cycleOf(solmin.getPayday(), cycle1.cycleStartDate().minusDays(1));
        BudgetCyclePolicy.BudgetCycle cycle3 =
                BudgetCyclePolicy.cycleOf(solmin.getPayday(), cycle2.cycleStartDate().minusDays(1));
        List<BudgetCyclePolicy.BudgetCycle> completedCyclesAsc = List.of(cycle3, cycle2, cycle1);

        solmin.startCardSync(now.minusMonths(4));
        Card card = seedCardConnection(solmin, now);

        seedExchangeRateCache(completedCyclesAsc);

        // ── 반복결제 탐지 후보 5종(이름형) + biller형 1종 = 후보상태 5가지 전부 커버 ──
        seedClaudeCandidatePending(solmin, card, categories, completedCyclesAsc);
        seedCoupangWowRegistered(solmin, card, categories, completedCyclesAsc, cycle0);
        seedRedCrossSnoozed(solmin, card, categories, completedCyclesAsc, cycle0);
        seedAppleClassifiedHabit(solmin, card, categories, completedCyclesAsc);
        seedWatchaDoNotRecommend(solmin, card, categories, completedCyclesAsc);
        FixedExpense netflix =
                seedNetflixMissedPayment(solmin, card, categories, completedCyclesAsc, cycle0);

        // ── 직접 등록(경로 B) — 카드형(아직 결제내역 없는 새 구독) + 계좌이체형(독립 사용) ──
        FixedExpense gym = seedGymDirectCard(solmin, categories, cycle0);
        seedRentDirectTransfer(solmin, categories);

        // ── 습관성 지출 코칭용 일반 소비 내역(직전 완료 주기에 집중, 다른 주기엔 옅게) ──
        seedHabitFillerTransactions(solmin, card, categories, cycle3, cycle2, cycle1, cycle0, today);

        cardTransactionRepository.flush();
        fixedExpensePaymentRepository.flush();

        // ── 예산 스냅샷(완료 3주기 + 진행 중 1주기) + 잉여금 + 수입 직접 입력 ──
        List<Budget> budgets = seedBudgets(solmin, List.of(cycle3, cycle2, cycle1, cycle0));
        seedSurplusFunds(solmin, completedCyclesAsc, budgets);
        seedIncomeAdjustments(budgets, cycle1, cycle0, today);

        // ── 습관 코칭 카드(직전 완료 주기 분석 결과) + 넘김 기록 ──
        seedCoachingCards(solmin, categories, cycle1, cycle0);

        // ── 결제 예정·미납 알림(고정지출 목록·알림함 UI 확인용) ──
        seedNotifications(solmin, gym, netflix, cycle0);
    }

    // ══════════════════════════ 카드 연동 ══════════════════════════

    private Card seedCardConnection(Member member, LocalDateTime now) {
        CardIssuer hyundai = cardIssuerRepository.findById("0302")
                .orElseThrow(() -> new IllegalStateException("현대카드(0302)가 시드되지 않았습니다."));
        CardConnection connection = cardConnectionRepository.save(
                CardConnection.connect(member, hyundai, "SEED-HYUNDAI-CONNECTED-0001", now.minusMonths(4)));
        connection.startInitialSync(now.minusMonths(4));
        connection.markInitialSyncAnalyzing();
        connection.completeInitialSync(now.minusMonths(4).plusHours(2));
        connection.markSynced(now.minusHours(6));
        return cardRepository.save(
                Card.observe(member, connection, "현대카드 the Green", "1234-****-****-5678"));
    }

    // ══════════════════════════ 환율 캐시 ══════════════════════════

    private void seedExchangeRateCache(List<BudgetCyclePolicy.BudgetCycle> completedCyclesAsc) {
        BigDecimal[] rates = {new BigDecimal("1372.30"), new BigDecimal("1391.80"), new BigDecimal("1385.50")};
        for (int i = 0; i < completedCyclesAsc.size(); i++) {
            LocalDate rateDate = completedCyclesAsc.get(i).cycleStartDate().plusDays(5);
            exchangeRateCacheRepository.save(
                    ExchangeRateCache.of(rateDate, "USD", rates[i], rateDate.atTime(9, 0)));
        }
    }

    // ══════════════════════════ ①Claude 구독 — PENDING ══════════════════════════

    private void seedClaudeCandidatePending(Member member, Card card, Map<CategoryCode, Category> categories,
                                             List<BudgetCyclePolicy.BudgetCycle> completedCyclesAsc) {
        MerchantAlias claudeAlias = findAliasOrThrow("Claude 구독");
        Merchant claudeMerchant = merchantRepository.save(Merchant.observe("CLAUDE.AI SUBSCRIPTION", claudeAlias));
        BigDecimal[] krwAmounts = {new BigDecimal("30190.60"), new BigDecimal("30619.60"), new BigDecimal("30481.00")};
        BigDecimal[] rates = {new BigDecimal("1372.30"), new BigDecimal("1391.80"), new BigDecimal("1385.50")};
        BigDecimal originalAmount = new BigDecimal("22.00");

        List<CardTransaction> txs = new ArrayList<>();
        for (int i = 0; i < completedCyclesAsc.size(); i++) {
            BudgetCyclePolicy.BudgetCycle cycle = completedCyclesAsc.get(i);
            LocalDate usedDate = cycle.cycleStartDate().plusDays(5);
            CardTransaction tx = cardTransactionRepository.save(overseasSyncTx(member, card,
                    categories.get(CategoryCode.SUBSCRIPTION), usedDate, usedDate.atTime(3, 12), krwAmounts[i],
                    originalAmount, "USD", rates[i], "CLAUDE.AI SUBSCRIPTION"));
            tx.assignMerchant(claudeMerchant, claudeAlias);
            txs.add(tx);
        }

        RecurringPaymentGroup group = recurringPaymentGroupRepository.save(RecurringPaymentGroup.detect(
                member, claudeAlias, (short) txs.size(), new BigDecimal("30430.40"), BigDecimal.ZERO,
                (short) lastDay(txs), (short) 1, lastDate(txs)));
        linkGroupTransactions(group, txs);

        fixedExpenseCandidateRepository.save(FixedExpenseCandidate.create(member, group,
                categories.get(CategoryCode.SUBSCRIPTION), new BigDecimal("88.50"),
                "Claude.ai 구독료가 3개월 연속 비슷한 날짜·금액으로 결제됐어요. 고정지출로 등록할까요?"));
    }

    // ══════════════════════════ ②쿠팡와우 — REGISTERED (+이행 내역) ══════════════════════════

    private FixedExpense seedCoupangWowRegistered(Member member, Card card, Map<CategoryCode, Category> categories,
                                                   List<BudgetCyclePolicy.BudgetCycle> completedCyclesAsc,
                                                   BudgetCyclePolicy.BudgetCycle cycle0) {
        MerchantAlias alias = findAliasOrThrow("쿠팡와우 멤버십");
        Merchant merchant = merchantRepository.save(Merchant.observe("쿠팡(쿠페이)", alias));
        BigDecimal amount = new BigDecimal("7890.00");

        List<CardTransaction> txs = seedNameGroupTransactions(member, card, categories.get(CategoryCode.SUBSCRIPTION),
                "쿠팡(쿠페이)", null, amount, 17, completedCyclesAsc, alias, merchant);

        RecurringPaymentGroup group = recurringPaymentGroupRepository.save(RecurringPaymentGroup.detect(
                member, alias, (short) txs.size(), amount, BigDecimal.ZERO, (short) lastDay(txs), (short) 1,
                lastDate(txs)));
        linkGroupTransactions(group, txs);

        FixedExpenseCandidate candidate = fixedExpenseCandidateRepository.save(FixedExpenseCandidate.create(
                member, group, categories.get(CategoryCode.SUBSCRIPTION), new BigDecimal("92.00"),
                "쿠팡와우 멤버십이 매달 비슷한 날짜에 7,890원으로 결제되고 있어요. 고정지출로 등록할까요?"));
        candidate.register();

        FixedExpense fixedExpense = fixedExpenseRepository.save(FixedExpense.fromCandidate(member, candidate,
                "쿠팡와우 멤버십", categories.get(CategoryCode.SUBSCRIPTION), alias, null,
                (short) lastDay(txs), amount, KRW, amount, null, null));

        for (int i = 0; i < txs.size(); i++) {
            CardTransaction tx = txs.get(i);
            tx.assignFixedExpense(fixedExpense);
            fixedExpensePaymentRepository.save(FixedExpensePayment.paid(fixedExpense,
                    completedCyclesAsc.get(i).yearMonth(), tx, amount));
        }
        // cycle0(진행 중 주기)은 의도적으로 미결제 상태로 남겨 둔다 — 다음 결제 예정 UI 확인용.
        return fixedExpense;
    }

    // ══════════════════════════ ③대한적십자사 — EXCLUDED_THIS_MONTH(스누즈) ══════════════════════════

    private void seedRedCrossSnoozed(Member member, Card card, Map<CategoryCode, Category> categories,
                                      List<BudgetCyclePolicy.BudgetCycle> completedCyclesAsc,
                                      BudgetCyclePolicy.BudgetCycle cycle0) {
        MerchantAlias alias = findAliasOrThrow("대한적십자사 정기후원");
        Merchant merchant = merchantRepository.save(Merchant.observe("대한적십자사", alias));
        BigDecimal amount = new BigDecimal("30000.00");

        List<CardTransaction> txs = seedNameGroupTransactions(member, card, categories.get(CategoryCode.DONATION),
                "대한적십자사", "각종회비", amount, 27, completedCyclesAsc, alias, merchant);

        RecurringPaymentGroup group = recurringPaymentGroupRepository.save(RecurringPaymentGroup.detect(
                member, alias, (short) txs.size(), amount, BigDecimal.ZERO, (short) lastDay(txs), (short) 1,
                lastDate(txs)));
        linkGroupTransactions(group, txs);

        FixedExpenseCandidate candidate = fixedExpenseCandidateRepository.save(FixedExpenseCandidate.create(
                member, group, categories.get(CategoryCode.DONATION), new BigDecimal("90.00"),
                "대한적십자사 정기후원이 매달 비슷한 금액으로 결제되고 있어요. 고정지출로 등록할까요?"));
        candidate.snooze(cycle0.yearMonth());
    }

    // ══════════════════════════ ④Apple→웨이브 — CLASSIFIED_HABIT(biller형) ══════════════════════════

    private void seedAppleClassifiedHabit(Member member, Card card, Map<CategoryCode, Category> categories,
                                           List<BudgetCyclePolicy.BudgetCycle> completedCyclesAsc) {
        Merchant appleMerchant = merchantRepository.save(Merchant.observe("Apple", null));
        BigDecimal amount = new BigDecimal("6300.00");

        List<CardTransaction> txs = new ArrayList<>();
        for (BudgetCyclePolicy.BudgetCycle cycle : completedCyclesAsc) {
            LocalDate usedDate = cycle.cycleStartDate().plusDays(16);
            CardTransaction tx = cardTransactionRepository.save(domesticSyncTx(member, card,
                    categories.get(CategoryCode.ETC), usedDate, usedDate.atTime(21, 5), amount, "Apple", null));
            tx.assignMerchant(appleMerchant, null);
            txs.add(tx);
        }
        // 같은 가맹점(Apple)에 섞인 일회성 결제 — §5 조건④(주기 1회 매칭)가 이걸 걸러내야 한다(fixedExpense.md §7).
        LocalDate oneOff1 = completedCyclesAsc.get(1).cycleStartDate().plusDays(4);
        CardTransaction oneOffTx1 = cardTransactionRepository.save(domesticSyncTx(member, card,
                categories.get(CategoryCode.ETC), oneOff1, oneOff1.atTime(14, 40), new BigDecimal("15000.00"),
                "Apple", null));
        oneOffTx1.assignMerchant(appleMerchant, null);
        LocalDate oneOff2 = completedCyclesAsc.get(2).cycleStartDate().plusDays(9);
        CardTransaction oneOffTx2 = cardTransactionRepository.save(domesticSyncTx(member, card,
                categories.get(CategoryCode.ETC), oneOff2, oneOff2.atTime(11, 15), new BigDecimal("22000.00"),
                "Apple", null));
        oneOffTx2.assignMerchant(appleMerchant, null);

        RecurringPaymentGroup group = recurringPaymentGroupRepository.save(RecurringPaymentGroup.detectBiller(
                member, appleMerchant, (short) txs.size(), amount, BigDecimal.ZERO, (short) lastDay(txs), (short) 1,
                lastDate(txs)));
        linkGroupTransactions(group, txs);

        FixedExpenseCandidate candidate = fixedExpenseCandidateRepository.save(FixedExpenseCandidate.create(
                member, group, categories.get(CategoryCode.SUBSCRIPTION), new BigDecimal("80.00"),
                "Apple을 통한 6,300원 결제가 매달 반복되고 있어요. 고정지출로 등록할까요?"));
        candidate.classifyHabit();

        userMerchantPreferenceRepository.save(
                UserMerchantPreference.createForBiller(member, appleMerchant, UserMerchantPreferenceType.RECLASSIFY_HABIT));
    }

    // ══════════════════════════ ⑤왓챠 — DO_NOT_RECOMMEND ══════════════════════════

    private void seedWatchaDoNotRecommend(Member member, Card card, Map<CategoryCode, Category> categories,
                                           List<BudgetCyclePolicy.BudgetCycle> completedCyclesAsc) {
        MerchantAlias alias = merchantAliasRepository.save(
                MerchantAlias.create("왓챠 구독", categories.get(CategoryCode.SUBSCRIPTION)));
        merchantAliasTermsRepository.save(MerchantAliasTerms.seed(alias, "WATCHA"));
        Merchant merchant = merchantRepository.save(Merchant.observe("WATCHA", alias));
        BigDecimal amount = new BigDecimal("12900.00");

        List<CardTransaction> txs = seedNameGroupTransactions(member, card, categories.get(CategoryCode.SUBSCRIPTION),
                "WATCHA", null, amount, 10, completedCyclesAsc, alias, merchant);

        RecurringPaymentGroup group = recurringPaymentGroupRepository.save(RecurringPaymentGroup.detect(
                member, alias, (short) txs.size(), amount, BigDecimal.ZERO, (short) lastDay(txs), (short) 1,
                lastDate(txs)));
        linkGroupTransactions(group, txs);

        FixedExpenseCandidate candidate = fixedExpenseCandidateRepository.save(FixedExpenseCandidate.create(
                member, group, categories.get(CategoryCode.SUBSCRIPTION), new BigDecimal("85.00"),
                "왓챠 구독료가 매달 결제되고 있어요. 고정지출로 등록할까요?"));
        candidate.reject();

        userMerchantPreferenceRepository.save(
                UserMerchantPreference.create(member, alias, UserMerchantPreferenceType.DO_NOT_RECOMMEND));
    }

    // ══════════════════════════ ⑥넷플릭스 — REGISTERED + 이번 주기 미납(알림 데모) ══════════════════════════

    private FixedExpense seedNetflixMissedPayment(Member member, Card card, Map<CategoryCode, Category> categories,
                                                   List<BudgetCyclePolicy.BudgetCycle> completedCyclesAsc,
                                                   BudgetCyclePolicy.BudgetCycle cycle0) {
        MerchantAlias alias = merchantAliasRepository.save(
                MerchantAlias.create("넷플릭스 스탠다드", categories.get(CategoryCode.SUBSCRIPTION)));
        merchantAliasTermsRepository.save(MerchantAliasTerms.seed(alias, "NETFLIX.COM"));
        Merchant merchant = merchantRepository.save(Merchant.observe("NETFLIX.COM", alias));
        BigDecimal amount = new BigDecimal("17000.00");

        List<CardTransaction> txs = seedNameGroupTransactions(member, card, categories.get(CategoryCode.SUBSCRIPTION),
                "NETFLIX.COM", null, amount, 8, completedCyclesAsc, alias, merchant);

        RecurringPaymentGroup group = recurringPaymentGroupRepository.save(RecurringPaymentGroup.detect(
                member, alias, (short) txs.size(), amount, BigDecimal.ZERO, (short) lastDay(txs), (short) 1,
                lastDate(txs)));
        linkGroupTransactions(group, txs);

        FixedExpenseCandidate candidate = fixedExpenseCandidateRepository.save(FixedExpenseCandidate.create(
                member, group, categories.get(CategoryCode.SUBSCRIPTION), new BigDecimal("91.50"),
                "넷플릭스 구독료가 매달 비슷한 날짜에 결제되고 있어요. 고정지출로 등록할까요?"));
        candidate.register();

        FixedExpense fixedExpense = fixedExpenseRepository.save(FixedExpense.fromCandidate(member, candidate,
                "넷플릭스 스탠다드", categories.get(CategoryCode.SUBSCRIPTION), alias, null,
                (short) lastDay(txs), amount, KRW, amount, null, null));

        for (int i = 0; i < txs.size(); i++) {
            CardTransaction tx = txs.get(i);
            tx.assignFixedExpense(fixedExpense);
            fixedExpensePaymentRepository.save(FixedExpensePayment.paid(fixedExpense,
                    completedCyclesAsc.get(i).yearMonth(), tx, amount));
        }
        // cycle0에는 일부러 결제 거래를 만들지 않는다 — 미납 알림·"결제가 안 보여요" UI를 항상 재현할 수 있게.
        return fixedExpense;
    }

    // ══════════════════════════ 직접 등록(경로 B) ══════════════════════════

    private FixedExpense seedGymDirectCard(Member member, Map<CategoryCode, Category> categories,
                                            BudgetCyclePolicy.BudgetCycle cycle0) {
        MerchantAlias alias = merchantAliasRepository.save(
                MerchantAlias.create("헬스장 정기결제", categories.get(CategoryCode.LEISURE)));
        // 결제내역이 아직 없는 상태로 직접 등록한다(fixedExpense.md §4 "아직 결제내역이 없는 새 구독").
        return fixedExpenseRepository.save(FixedExpense.registerDirect(member, "헬스장 정기결제",
                categories.get(CategoryCode.LEISURE), alias, FixedExpensePaymentMethod.CARD,
                (short) 1, new BigDecimal("89000.00"), KRW, new BigDecimal("89000.00"), null, null));
    }

    private FixedExpense seedRentDirectTransfer(Member member, Map<CategoryCode, Category> categories) {
        // 계좌이체형(월세)은 카드 승인내역에 안 잡혀 merchantAlias 없이도 등록 가능하다(fixedExpense.md §4·§7).
        return fixedExpenseRepository.save(FixedExpense.registerDirect(member, "월세",
                categories.get(CategoryCode.ETC), null, FixedExpensePaymentMethod.TRANSFER,
                (short) 25, new BigDecimal("500000.00"), KRW, new BigDecimal("500000.00"), null, null));
    }

    // ══════════════════════════ 습관성 지출 코칭용 일반 소비 필러 ══════════════════════════

    private void seedHabitFillerTransactions(Member member, Card card, Map<CategoryCode, Category> categories,
                                              BudgetCyclePolicy.BudgetCycle cycle3, BudgetCyclePolicy.BudgetCycle cycle2,
                                              BudgetCyclePolicy.BudgetCycle cycle1, BudgetCyclePolicy.BudgetCycle cycle0,
                                              LocalDate today) {
        // 직전 완료 주기(cycle1) — 습관 코칭 배치의 실제 분석 대상. §3 실측 사례(편의점·배달·외식 상위,
        // 택시·PC방은 월 3만원 하한 미달로 제외)를 그대로 재현한다.
        seedFiller(member, card, categories, cycle1, CategoryCode.CONVENIENCE, "편의점", "편의점",
                new int[]{2, 5, 8, 11, 14, 18, 22, 26}, new int[]{3900, 5200, 4700, 8900, 6100, 7300, 5500, 9200});
        seedFiller(member, card, categories, cycle1, CategoryCode.DELIVERY, "쿠팡이츠", null,
                new int[]{4, 9, 15, 21}, new int[]{18900, 23400, 15600, 21000});
        seedFiller(member, card, categories, cycle1, CategoryCode.FOOD, "한식", "한식",
                new int[]{3, 7, 12, 16, 20, 24}, new int[]{12000, 8900, 15400, 9800, 22000, 11300});
        seedFiller(member, card, categories, cycle1, CategoryCode.CAFE, "스타벅스", "커피전문점",
                new int[]{2, 6, 10, 13, 19, 23}, new int[]{4800, 5200, 4500, 6100, 5000, 4900});
        seedFiller(member, card, categories, cycle1, CategoryCode.TRANSPORT, "택시", "택시",
                new int[]{8, 20}, new int[]{8500, 7200});
        seedFiller(member, card, categories, cycle1, CategoryCode.LEISURE, "PC방", "PC게임방",
                new int[]{6, 17}, new int[]{9000, 8600});
        seedFiller(member, card, categories, cycle1, CategoryCode.SHOPPING, "쿠팡", null,
                new int[]{9, 25}, new int[]{27000, 39000});

        // 이전 주기들 — 소비내역·TOP5 화면에 두께를 주는 정도로 옅게.
        seedFiller(member, card, categories, cycle2, CategoryCode.FOOD, "일반대중음식", "일반대중음식",
                new int[]{6, 19}, new int[]{11000, 9500});
        seedFiller(member, card, categories, cycle2, CategoryCode.CAFE, "이디야", "커피전문점",
                new int[]{4, 21}, new int[]{5000, 4800});
        seedFiller(member, card, categories, cycle2, CategoryCode.CONVENIENCE, "GS25", "편의점",
                new int[]{8, 24}, new int[]{6000, 5500});
        seedFiller(member, card, categories, cycle2, CategoryCode.TRANSPORT, "택시", "택시",
                new int[]{15}, new int[]{9000});
        seedFiller(member, card, categories, cycle2, CategoryCode.SHOPPING, "쿠팡", null,
                new int[]{11}, new int[]{32000});

        seedFiller(member, card, categories, cycle3, CategoryCode.FOOD, "중식", "중식",
                new int[]{5, 22}, new int[]{10500, 13000});
        seedFiller(member, card, categories, cycle3, CategoryCode.CAFE, "투썸플레이스", "커피전문점",
                new int[]{9, 20}, new int[]{4700, 5300});
        seedFiller(member, card, categories, cycle3, CategoryCode.CONVENIENCE, "CU", "편의점",
                new int[]{7, 26}, new int[]{5800, 6200});
        seedFiller(member, card, categories, cycle3, CategoryCode.DELIVERY, "쿠팡이츠", null,
                new int[]{13}, new int[]{19500});
        seedFiller(member, card, categories, cycle3, CategoryCode.SHOPPING, "쿠팡", null,
                new int[]{18}, new int[]{28000});

        // 진행 중 주기(cycle0) — "오늘 쓴 돈"·최근 내역 화면용. today를 넘지 않게 clamp한다.
        seedFillerClamped(member, card, categories, cycle0, today, CategoryCode.FOOD, "한식", "한식",
                new int[]{1, 4}, new int[]{9800, 12500});
        seedFillerClamped(member, card, categories, cycle0, today, CategoryCode.CAFE, "스타벅스", "커피전문점",
                new int[]{0, 3}, new int[]{4500, 5100});
        seedFillerClamped(member, card, categories, cycle0, today, CategoryCode.CONVENIENCE, "편의점", "편의점",
                new int[]{2}, new int[]{6300});
        seedFillerClamped(member, card, categories, cycle0, today, CategoryCode.DELIVERY, "쿠팡이츠", null,
                new int[]{1}, new int[]{17800});
    }

    private void seedFiller(Member member, Card card, Map<CategoryCode, Category> categories,
                             BudgetCyclePolicy.BudgetCycle cycle, CategoryCode categoryCode, String merchantNameRaw,
                             String merchantTypeRaw, int[] dayOffsets, int[] amounts) {
        for (int i = 0; i < dayOffsets.length; i++) {
            LocalDate usedDate = clampToCycle(cycle, cycle.cycleStartDate().plusDays(dayOffsets[i]));
            LocalDateTime usedAt = usedDate.atTime(11 + (i % 9), 15 + (i * 7) % 40);
            cardTransactionRepository.save(domesticSyncTx(member, card, categories.get(categoryCode), usedDate,
                    usedAt, BigDecimal.valueOf(amounts[i]).setScale(2), merchantNameRaw, merchantTypeRaw));
        }
    }

    private void seedFillerClamped(Member member, Card card, Map<CategoryCode, Category> categories,
                                    BudgetCyclePolicy.BudgetCycle cycle0, LocalDate today, CategoryCode categoryCode,
                                    String merchantNameRaw, String merchantTypeRaw, int[] dayOffsets, int[] amounts) {
        for (int i = 0; i < dayOffsets.length; i++) {
            LocalDate usedDate = clampDate(cycle0.cycleStartDate().plusDays(dayOffsets[i]), cycle0.cycleStartDate(), today);
            LocalDateTime usedAt = usedDate.atTime(9 + (i % 9), 10 + (i * 7) % 40);
            if (usedAt.isAfter(LocalDateTime.now())) {
                usedAt = LocalDateTime.now().minusMinutes(5L * (i + 1));
            }
            cardTransactionRepository.save(domesticSyncTx(member, card, categories.get(categoryCode), usedDate,
                    usedAt, BigDecimal.valueOf(amounts[i]).setScale(2), merchantNameRaw, merchantTypeRaw));
        }
    }

    // ══════════════════════════ 예산·잉여금·수입 직접 입력 ══════════════════════════

    private List<Budget> seedBudgets(Member member, List<BudgetCyclePolicy.BudgetCycle> cyclesAsc) {
        BigDecimal expectedFixedTotal = fixedExpenseRepository
                .findByMember_IdAndStatus(member.getId(), FixedExpenseStatus.ACTIVE).stream()
                .map(FixedExpense::getExpectedAmountKrw)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Budget> budgets = new ArrayList<>();
        for (BudgetCyclePolicy.BudgetCycle cycle : cyclesAsc) {
            // 소비 가이드를 먼저 켜 본 이력이 있으면 그 주기의 budget row가 이미 지연 생성돼 있을 수 있다
            // (BudgetCycleService.getOrCreateCurrentBudget). 고정지출 스냅샷을 반영한 값으로 다시 세팅한다.
            budgetRepository.findByMember_IdAndYearMonth(member.getId(), cycle.yearMonth())
                    .ifPresent(budgetRepository::delete);
            budgetRepository.flush();
            BigDecimal confirmed = fixedExpensePaymentRepository.sumPaidAmount(member.getId(), cycle.yearMonth());
            budgets.add(budgetRepository.save(Budget.openCycle(member, cycle.yearMonth(), cycle.cycleStartDate(),
                    cycle.cycleEndDate(), member.getSalaryAmount(), member.getSavingsTargetOrZero(),
                    expectedFixedTotal, confirmed)));
        }
        return budgets;
    }

    private void seedSurplusFunds(Member member, List<BudgetCyclePolicy.BudgetCycle> completedCyclesAsc,
                                   List<Budget> budgetsAsc) {
        for (int i = 0; i < completedCyclesAsc.size(); i++) {
            BudgetCyclePolicy.BudgetCycle cycle = completedCyclesAsc.get(i);
            if (surplusFundRepository.existsByMember_IdAndYearMonth(member.getId(), cycle.yearMonth())) {
                continue;
            }
            Budget budget = budgetsAsc.get(i);
            BigDecimal netSpend = cardTransactionRepository.sumNetSpend(member.getId(), TransactionStatus.REJECTED,
                    cycle.cycleStartDate(), cycle.cycleEndDate());
            BigDecimal surplus = budget.getAvailableAmount(BigDecimal.ZERO).subtract(netSpend);
            surplusFundRepository.save(SurplusFund.accrue(member, cycle.yearMonth(), surplus, cycle.cycleEndDate(),
                    cycle.cycleEndDate().atTime(4, 0)));
        }
    }

    private void seedIncomeAdjustments(List<Budget> budgetsAsc, BudgetCyclePolicy.BudgetCycle cycle1,
                                        BudgetCyclePolicy.BudgetCycle cycle0, LocalDate today) {
        Budget cycle1Budget = budgetsAsc.get(2);
        Budget cycle0Budget = budgetsAsc.get(3);
        LocalDate cycle1AdjDate = cycle1.cycleStartDate().plusDays(10);
        budgetIncomeAdjustmentRepository.save(BudgetIncomeAdjustment.create(cycle1Budget,
                new BigDecimal("100000.00"), "생일 용돈", cycle1AdjDate, cycle1AdjDate.atTime(20, 0)));

        LocalDate cycle0AdjDate = clampDate(today.minusDays(1), cycle0.cycleStartDate(), today);
        budgetIncomeAdjustmentRepository.save(BudgetIncomeAdjustment.create(cycle0Budget,
                new BigDecimal("50000.00"), "연말정산 환급금", cycle0AdjDate, cycle0AdjDate.atTime(19, 30)));
    }

    // ══════════════════════════ 습관 코칭 카드 ══════════════════════════

    private void seedCoachingCards(Member member, Map<CategoryCode, Category> categories,
                                    BudgetCyclePolicy.BudgetCycle analyzedCycle, BudgetCyclePolicy.BudgetCycle createdCycle) {
        if (coachingCardRepository.existsByMember_IdAndYearMonth(member.getId(), analyzedCycle.yearMonth())) {
            return;
        }
        LocalDateTime createdAt = createdCycle.cycleStartDate().atTime(4, 30);
        coachingCardRepository.save(CoachingCard.forCategory(member, analyzedCycle.yearMonth(),
                "외식이 이번 주기 소비 1위예요",
                "직전 급여주기 동안 외식으로 6번, 총 79,400원을 쓰셨어요. 도시락이나 집밥을 조금 더 늘려보는 건 어떨까요?",
                categories.get(CategoryCode.FOOD), new BigDecimal("23820.00"), (short) 1, createdAt));
        coachingCardRepository.save(CoachingCard.forCategory(member, analyzedCycle.yearMonth(),
                "배달 음식 주문이 꽤 잦았어요",
                "직전 급여주기 동안 배달 앱으로 4번, 총 78,900원을 쓰셨어요. 가끔은 포장해서 가져오는 것도 괜찮을 것 같아요.",
                categories.get(CategoryCode.DELIVERY), new BigDecimal("39450.00"), (short) 2, createdAt));
        coachingCardRepository.save(CoachingCard.forCategory(member, analyzedCycle.yearMonth(),
                "편의점 방문이 잦았어요",
                "직전 급여주기 동안 편의점에서 8번, 총 50,800원을 쓰셨어요. 필요한 물건만 미리 챙겨두면 방문 횟수를 줄일 수 있어요.",
                categories.get(CategoryCode.CONVENIENCE), new BigDecimal("15240.00"), (short) 3, createdAt));

        // 이전 주기에 "이건 낭비 아니에요"로 넘긴 카테고리 — 다음 코칭 배치가 톤다운해야 할 대상.
        coachingDismissRepository.save(CoachingDismiss.forCategory(member, categories.get(CategoryCode.CAFE),
                CoachingDismissType.NOT_WASTE, createdAt.minusDays(1)));
    }

    // ══════════════════════════ 알림 ══════════════════════════

    private void seedNotifications(Member member, FixedExpense gym, FixedExpense netflix,
                                    BudgetCyclePolicy.BudgetCycle cycle0) {
        notificationService.createIfAbsent(member, NotificationTypeCode.PAYMENT_DUE,
                "헬스장 정기결제 결제 예정일이에요. 예상 금액 89,000원입니다.", gym, null,
                "PAYMENT_DUE:fe=" + gym.getId() + ":" + cycle0.yearMonth());
        notificationService.createIfAbsent(member, NotificationTypeCode.MISSED_PAYMENT,
                "이번 달 넷플릭스 스탠다드 결제가 안 보여요. 카드 결제내역을 확인해 주세요.", netflix, null,
                "MISSED_PAYMENT:fe=" + netflix.getId() + ":" + cycle0.yearMonth());
    }

    // ══════════════════════════ 공용 헬퍼 ══════════════════════════

    private List<CardTransaction> seedNameGroupTransactions(Member member, Card card, Category category,
                                                              String merchantNameRaw, String merchantTypeRaw,
                                                              BigDecimal amount, int dayOffset,
                                                              List<BudgetCyclePolicy.BudgetCycle> cyclesAsc,
                                                              MerchantAlias alias, Merchant merchant) {
        List<CardTransaction> txs = new ArrayList<>();
        for (BudgetCyclePolicy.BudgetCycle cycle : cyclesAsc) {
            LocalDate usedDate = cycle.cycleStartDate().plusDays(dayOffset);
            CardTransaction tx = cardTransactionRepository.save(domesticSyncTx(member, card, category, usedDate,
                    usedDate.atTime(20, 15), amount, merchantNameRaw, merchantTypeRaw));
            tx.assignMerchant(merchant, alias);
            txs.add(tx);
        }
        return txs;
    }

    private void linkGroupTransactions(RecurringPaymentGroup group, List<CardTransaction> txs) {
        for (CardTransaction tx : txs) {
            recurringPaymentGroupTransactionRepository.save(
                    RecurringPaymentGroupTransaction.of(group.getId(), tx.getId()));
        }
    }

    private CardTransaction domesticSyncTx(Member member, Card card, Category category, LocalDate usedDate,
                                            LocalDateTime usedAt, BigDecimal amount, String merchantNameRaw,
                                            String merchantTypeRaw) {
        return CardTransaction.sync(member, card, category, nextDomesticApprovalNo(), usedDate, usedAt, amount,
                null, KRW, null, false, TransactionStatus.APPROVED, null, false,
                merchantNameRaw, merchantTypeRaw, null, null, null);
    }

    private CardTransaction overseasSyncTx(Member member, Card card, Category category, LocalDate usedDate,
                                            LocalDateTime usedAt, BigDecimal krwAmount, BigDecimal originalAmount,
                                            String currencyCode, BigDecimal exchangeRate, String merchantNameRaw) {
        return CardTransaction.sync(member, card, category, nextOverseasApprovalNo(), usedDate, usedAt, krwAmount,
                originalAmount, currencyCode, exchangeRate, false, TransactionStatus.APPROVED, null, false,
                merchantNameRaw, null, null, null, null);
    }

    private String nextDomesticApprovalNo() {
        return String.valueOf(domesticApprovalSeq.getAndIncrement());
    }

    private String nextOverseasApprovalNo() {
        return String.valueOf(overseasApprovalSeq.getAndIncrement());
    }

    private MerchantAlias findAliasOrThrow(String canonicalServiceName) {
        return merchantAliasRepository.findByCanonicalServiceNameNormalized(canonicalServiceName)
                .orElseThrow(() -> new IllegalStateException(canonicalServiceName + " alias가 시드되지 않았습니다."));
    }

    private Map<CategoryCode, Category> loadCategories() {
        Map<CategoryCode, Category> categories = new EnumMap<>(CategoryCode.class);
        for (CategoryCode code : CategoryCode.values()) {
            categoryRepository.findByCode(code.name()).ifPresent(category -> categories.put(code, category));
        }
        return categories;
    }

    private int lastDay(List<CardTransaction> txs) {
        return txs.get(txs.size() - 1).getUsedDate().getDayOfMonth();
    }

    private LocalDate lastDate(List<CardTransaction> txs) {
        return txs.get(txs.size() - 1).getUsedDate();
    }

    /** cycle 범위 안으로만 당긴다(진행 중 주기는 cycleEndDate가 미래라 별도 clamp가 필요 없다). */
    private LocalDate clampToCycle(BudgetCyclePolicy.BudgetCycle cycle, LocalDate candidate) {
        return clampDate(candidate, cycle.cycleStartDate(), cycle.cycleEndDate());
    }

    private LocalDate clampDate(LocalDate candidate, LocalDate min, LocalDate max) {
        if (candidate.isBefore(min)) {
            return min;
        }
        return candidate.isAfter(max) ? max : candidate;
    }
}
