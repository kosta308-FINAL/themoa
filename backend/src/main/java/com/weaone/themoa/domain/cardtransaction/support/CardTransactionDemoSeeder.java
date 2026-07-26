package com.weaone.themoa.domain.cardtransaction.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weaone.themoa.domain.budget.dto.request.SpendingGuideSetupRequest;
import com.weaone.themoa.domain.budget.entity.Budget;
import com.weaone.themoa.domain.budget.repository.BudgetRepository;
import com.weaone.themoa.domain.budget.service.BudgetCyclePolicy;
import com.weaone.themoa.domain.budget.service.SpendingGuideService;
import com.weaone.themoa.domain.budget.service.SurplusFundBatchService;
import com.weaone.themoa.domain.cardconnection.entity.CardConnection;
import com.weaone.themoa.domain.cardconnection.entity.CardIssuer;
import com.weaone.themoa.domain.cardconnection.repository.CardConnectionRepository;
import com.weaone.themoa.domain.cardconnection.repository.CardIssuerRepository;
import com.weaone.themoa.domain.cardtransaction.client.CodefApprovalRecord;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.cardtransaction.service.CardTransactionCollectionService;
import com.weaone.themoa.domain.fixedexpense.service.FixedExpenseDetectionService;
import com.weaone.themoa.domain.member.entity.IncomeType;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.member.support.MemberDemoSeeder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 시연용 카드 거래내역 시드(member_id=1, email="test"). {@code seed/demo-card-transactions.json}(실제 CODEF
 * 승인내역 응답에서 4/1~7/19는 그대로, 7/20~7/28은 고정지출 3종(클로드 구독·Apple·쿠팡와우 멤버십)을 제외한
 * 과거 구간을 날짜만 이동해 채운 것)을 실제 수집 파이프라인({@link CardTransactionCollectionService})에 그대로
 * 흘려보낸다 — 가맹점 신원 판별·카테고리 분류를 손으로 흉내 내지 않고 운영 코드와 동일한 경로로 저장한다.
 *
 * <p>{@link MemberDemoSeeder}(@Order 4)가 member_id=1을 먼저 만들어 둬야 하므로 그보다 뒤에 돈다.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class CardTransactionDemoSeeder implements ApplicationRunner {

    private static final String SEED_RESOURCE = "seed/demo-card-transactions.json";
    private static final String CARD_ISSUER_ORGANIZATION = "0306"; // 신한카드(실제 데모 카드 발급사)
    private static final String DEMO_CONNECTED_ID = "DEMO-SEED-CONNECTED-ID";
    private static final BigDecimal DEMO_SALARY_AMOUNT = new BigDecimal("2900000");
    private static final int DEMO_PAYDAY = 5;

    private final MemberRepository memberRepository;
    private final CardConnectionRepository cardConnectionRepository;
    private final CardIssuerRepository cardIssuerRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final CardTransactionCollectionService cardTransactionCollectionService;
    private final FixedExpenseDetectionService fixedExpenseDetectionService;
    private final SpendingGuideService spendingGuideService;
    private final BudgetRepository budgetRepository;
    private final SurplusFundBatchService surplusFundBatchService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        Member member = memberRepository.findByEmail(MemberDemoSeeder.DEMO_EMAIL).orElse(null);
        if (member == null || cardTransactionRepository.existsByMember_Id(member.getId())) {
            return;
        }

        CardIssuer cardIssuer = cardIssuerRepository.findById(CARD_ISSUER_ORGANIZATION)
                .orElseThrow(() -> new IllegalStateException(CARD_ISSUER_ORGANIZATION + " 카드사가 시드되지 않았습니다."));
        CardConnection cardConnection = cardConnectionRepository
                .findByMember_IdAndCardIssuer_Organization(member.getId(), CARD_ISSUER_ORGANIZATION)
                .orElseGet(() -> cardConnectionRepository.save(
                        CardConnection.connect(member, cardIssuer, DEMO_CONNECTED_ID, LocalDateTime.now())));

        List<CodefApprovalRecord> records = loadRecords();
        int created = 0;
        for (CodefApprovalRecord record : records) {
            cardTransactionCollectionService.collect(member, cardConnection, cardIssuer, record);
            created++;
        }
        log.info("데모 카드 거래내역 {}건 시드 완료(member_id={})", created, member.getId());

        // entry_mode는 MANUAL로 유지한다(startCardSync 호출 안 함) — 시연 계정은 UI상 수기모드를 유지하되
        // 더미데이터만 카드 연동이 끝난 것처럼 보이도록 초기수집 상태를 직접 COMPLETED로 마무리 짓는다.
        // 이걸 안 하면 initial_sync_status가 NOT_STARTED로 남아 프론트 InitialSyncView가 "카드 소비내역을
        // 동기화하고 있어요" 스피너를 영구히 띄운다.
        LocalDateTime now = LocalDateTime.now();
        cardConnection.startInitialSync(now);
        cardConnection.markInitialSyncAnalyzing();
        cardConnection.completeInitialSync(now);
        cardConnection.markSynced(now);

        // S-00A 최초 설정과 동일한 경로로 월급·급여일을 채운다 — card_connection이 이미 COMPLETED라
        // setup() 내부에서 과거 급여주기 budget도 함께 소급 생성된다(SpendingGuideService.setup 참고).
        spendingGuideService.setup(member.getId(),
                new SpendingGuideSetupRequest(IncomeType.SALARY, DEMO_SALARY_AMOUNT, null, null, DEMO_PAYDAY));
        log.info("데모 소비가이드 설정 완료(member_id={}, salary={}, payday={})", member.getId(), DEMO_SALARY_AMOUNT, DEMO_PAYDAY);

        // 잉여금 적립은 원래 04:30 새벽 배치(SurplusFundBatchService)가 종료된 주기를 훑으며 채우는데,
        // 방금 소급 생성한 과거 주기들은 그 배치가 아직 한 번도 안 돈 상태라 surplus_fund가 비어 있다
        // ("완료된 주기 합산"이 "-"로 보이는 원인). 배치를 기다리지 않고 지금 바로 같은 로직을 돌려 채운다.
        LocalDate today = LocalDate.now(BudgetCyclePolicy.ZONE_SEOUL);
        List<Budget> endedCycles = budgetRepository.findByMember_IdAndCycleEndDateBefore(member.getId(), today);
        for (Budget budget : endedCycles) {
            surplusFundBatchService.accrueIfAbsent(budget, today);
        }
        log.info("데모 잉여금 적립 완료(member_id={}, 완료주기={}건)", member.getId(), endedCycles.size());

        fixedExpenseDetectionService.detectForMember(member.getId());
        log.info("데모 고정지출 후보 탐지 완료(member_id={})", member.getId());
    }

    private List<CodefApprovalRecord> loadRecords() throws IOException {
        try (InputStream in = new ClassPathResource(SEED_RESOURCE).getInputStream()) {
            SeedFile seedFile = objectMapper.readValue(in, SeedFile.class);
            return seedFile.data().stream()
                    .sorted(Comparator.comparing(CodefApprovalRecord::resUsedDate)
                            .thenComparing(CodefApprovalRecord::resUsedTime))
                    .toList();
        }
    }

    private record SeedFile(List<CodefApprovalRecord> data) {
    }
}
