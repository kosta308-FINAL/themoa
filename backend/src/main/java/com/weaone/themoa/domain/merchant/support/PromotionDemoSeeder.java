package com.weaone.themoa.domain.merchant.support;

import com.weaone.themoa.domain.cardconnection.entity.CardConnection;
import com.weaone.themoa.domain.cardconnection.entity.CardIssuer;
import com.weaone.themoa.domain.cardconnection.repository.CardConnectionRepository;
import com.weaone.themoa.domain.cardconnection.repository.CardIssuerRepository;
import com.weaone.themoa.domain.cardtransaction.client.CodefApprovalRecord;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.cardtransaction.service.CardTransactionCollectionService;
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

import java.time.LocalDate;
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
    private static final String TEST2_EMAIL = "test2";
    private static final DateTimeFormatter USED_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MemberRepository memberRepository;
    private final CardConnectionRepository cardConnectionRepository;
    private final CardIssuerRepository cardIssuerRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final CardTransactionCollectionService cardTransactionCollectionService;

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
        memberRepository.findByEmail(TEST2_EMAIL)
                .ifPresent(member -> seedIfAbsent(member, cardIssuer, "46195410****DEM2"));
    }

    private void seedIfAbsent(Member member, CardIssuer cardIssuer, String cardNo) {
        String approvalNo = "DEMO-WATCHA-" + member.getId();
        if (cardTransactionRepository.existsByApprovalNo(approvalNo)) {
            return;
        }
        CardConnection connection = cardConnectionRepository
                .findByMember_IdAndCardIssuer_Organization(member.getId(), CARD_ISSUER_ORGANIZATION)
                .orElse(null);
        if (connection == null) {
            log.warn("카드 연동이 없어 승격 데모 거래를 건너뜁니다. memberId={}", member.getId());
            return;
        }
        String usedDate = LocalDate.now().format(USED_DATE_FORMAT);
        CodefApprovalRecord record = new CodefApprovalRecord(usedDate, "093000", cardNo, "",
                DEMO_MERCHANT_NAME, "13900", "KRW", approvalNo, "1", "", "", "", "0", "", "", "");
        cardTransactionCollectionService.collect(member, connection, cardIssuer, record);
        log.info("승격 데모 거래 시드 완료(memberId={}, merchant={})", member.getId(), DEMO_MERCHANT_NAME);
    }
}
