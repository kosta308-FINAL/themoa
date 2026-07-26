package com.weaone.themoa.domain.merchant.support;

import com.weaone.themoa.domain.budget.dto.request.SpendingGuideSetupRequest;
import com.weaone.themoa.domain.budget.service.SpendingGuideService;
import com.weaone.themoa.domain.cardconnection.entity.CardConnection;
import com.weaone.themoa.domain.cardconnection.entity.CardIssuer;
import com.weaone.themoa.domain.cardconnection.repository.CardConnectionRepository;
import com.weaone.themoa.domain.cardconnection.repository.CardIssuerRepository;
import com.weaone.themoa.domain.cardtransaction.client.CodefApprovalRecord;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.cardtransaction.service.CardTransactionCollectionService;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.category.repository.CategoryRepository;
import com.weaone.themoa.domain.fixedexpense.dto.request.FixedExpenseDirectRegisterRequest;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpensePaymentMethod;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseStatus;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseRepository;
import com.weaone.themoa.domain.fixedexpense.service.FixedExpenseRegistrationService;
import com.weaone.themoa.domain.member.entity.IncomeType;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.member.support.MemberDemoSeeder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 전역 마스터 승격 대기목록 시연용 시드. test(member_id=1)와 test2(email="test2") 두 계정에 아직 아무도
 * 학습·승격하지 않은 같은 원본 가맹점명("(주)왓챠") 거래를 하나씩 심어, "한 명이 학습 → 관리자가 승격 →
 * 다른 회원의 미분류 거래가 자동으로 정리된다"는 흐름을 화면 조작만으로 시연할 수 있게 한다.
 * {@link CardTransactionDemoSeeder}처럼 실제 수집 파이프라인({@link CardTransactionCollectionService})을
 * 그대로 태워 merchant/merchant_alias 판별을 손으로 흉내 내지 않는다. 운영 배포에는 쓰지 않는다.
 */
@Slf4j
@Component
@Order(11)
@RequiredArgsConstructor
public class PromotionDemoSeeder implements ApplicationRunner {

    private static final String CARD_ISSUER_ORGANIZATION = "0306";
    private static final String DEMO_MERCHANT_NAME = "(주)왓챠";
    private static final String DEMO_FIXED_EXPENSE_NAME = "왓챠";
    private static final BigDecimal DEMO_AMOUNT = new BigDecimal("13900");
    private static final DateTimeFormatter USED_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final BigDecimal DEMO_SALARY_AMOUNT = new BigDecimal("2000000");
    private static final int DEMO_PAYDAY = 10;

    private final MemberRepository memberRepository;
    private final CardConnectionRepository cardConnectionRepository;
    private final CardIssuerRepository cardIssuerRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final CardTransactionCollectionService cardTransactionCollectionService;
    private final SpendingGuideService spendingGuideService;
    private final FixedExpenseRegistrationService fixedExpenseRegistrationService;
    private final FixedExpenseRepository fixedExpenseRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        CardIssuer cardIssuer = cardIssuerRepository.findById(CARD_ISSUER_ORGANIZATION).orElse(null);
        if (cardIssuer == null) {
            log.warn("{} 카드사가 시드되지 않아 승격 데모 거래를 건너뜁니다.", CARD_ISSUER_ORGANIZATION);
            return;
        }
        memberRepository.findByEmail(MemberDemoSeeder.DEMO_EMAIL)
                .ifPresent(member -> seedIfAbsent(member, cardIssuer, "46195410****DEM1"));
        memberRepository.findByEmail(MemberDemoSeeder.DEMO_EMAIL_2)
                .ifPresent(member -> {
                    seedIfAbsent(member, cardIssuer, "46195410****DEM2");
                    registerFixedExpenseIfAbsent(member);
                });
    }

    private void seedIfAbsent(Member member, CardIssuer cardIssuer, String cardNo) {
        String approvalNo = "DEMO-WATCHA-" + member.getId();
        if (cardTransactionRepository.existsByApprovalNo(approvalNo)) {
            return;
        }
        CardConnection connection = findOrCreateConnection(member, cardIssuer);
        ensureSpendingGuideSetUp(member);
        String usedDate = LocalDate.now().format(USED_DATE_FORMAT);
        CodefApprovalRecord record = new CodefApprovalRecord(usedDate, "093000", cardNo, "",
                DEMO_MERCHANT_NAME, DEMO_AMOUNT.toPlainString(), "KRW", approvalNo, "1", "", "", "", "0", "", "", "");
        cardTransactionCollectionService.collect(member, connection, cardIssuer, record);
        log.info("승격 데모 거래 시드 완료(memberId={}, merchant={})", member.getId(), DEMO_MERCHANT_NAME);
    }

    /**
     * test는 {@code CardTransactionDemoSeeder}가 만들어 둔 연동을 그대로 쓰고, 신규 계정(test2)은
     * 여기서 처음 연동을 만든다 — 프론트가 "동기화 중" 스피너에 멈추지 않도록 초기수집 상태를 바로
     * COMPLETED로 마무리한다({@code CardTransactionDemoSeeder}와 동일 패턴).
     */
    private CardConnection findOrCreateConnection(Member member, CardIssuer cardIssuer) {
        return cardConnectionRepository
                .findByMember_IdAndCardIssuer_Organization(member.getId(), CARD_ISSUER_ORGANIZATION)
                .orElseGet(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    CardConnection connection = cardConnectionRepository.save(
                            CardConnection.connect(member, cardIssuer, "DEMO-SEED-CONNECTED-ID-" + member.getId(), now));
                    connection.startInitialSync(now);
                    connection.markInitialSyncAnalyzing();
                    connection.completeInitialSync(now);
                    connection.markSynced(now);
                    return connection;
                });
    }

    /** test2처럼 온보딩(S-00A)을 거친 적 없는 신규 계정은 소비가이드 설정 전이면 대시보드가 막혀 있다. */
    private void ensureSpendingGuideSetUp(Member member) {
        if (member.getPayday() != null) {
            return;
        }
        spendingGuideService.setup(member.getId(),
                new SpendingGuideSetupRequest(IncomeType.SALARY, DEMO_SALARY_AMOUNT, null, null, DEMO_PAYDAY));
        log.info("승격 데모 소비가이드 설정 완료(memberId={})", member.getId());
    }

    /**
     * test2가 admin 승격 전에 이미 "왓챠" 고정지출을 등록해 둔 상태를 시드한다 — 그래야 admin이 승격을
     * 누르는 순간 {@link com.weaone.themoa.domain.merchant.service.AdminMerchantService}의 소급 재분류가
     * 이 고정지출까지 자동으로 완납 매칭한다. 결제일은 항상 오늘 날짜로 맞춰 어느 날 재기동해도 방금 심은
     * "(주)왓챠" 거래와 결제일 창(±window)이 어긋나지 않게 한다.
     */
    private void registerFixedExpenseIfAbsent(Member member) {
        boolean alreadyRegistered = fixedExpenseRepository
                .findByMember_IdAndStatus(member.getId(), FixedExpenseStatus.ACTIVE).stream()
                .anyMatch(fixedExpense -> DEMO_FIXED_EXPENSE_NAME.equals(fixedExpense.getName()));
        if (alreadyRegistered) {
            return;
        }
        Category subscriptionCategory = categoryRepository.findByCode(CategoryCode.SUBSCRIPTION.name())
                .orElseThrow(() -> new IllegalStateException("SUBSCRIPTION 카테고리가 시드되지 않았습니다."));
        FixedExpenseDirectRegisterRequest request = new FixedExpenseDirectRegisterRequest(
                DEMO_FIXED_EXPENSE_NAME, subscriptionCategory.getId(), FixedExpensePaymentMethod.CARD,
                null, DEMO_FIXED_EXPENSE_NAME, DEMO_AMOUNT, "KRW",
                (short) LocalDate.now().getDayOfMonth());
        fixedExpenseRegistrationService.registerDirect(member.getId(), request);
        log.info("승격 데모 고정지출 시드 완료(memberId={}, name={})", member.getId(), DEMO_FIXED_EXPENSE_NAME);
    }
}
