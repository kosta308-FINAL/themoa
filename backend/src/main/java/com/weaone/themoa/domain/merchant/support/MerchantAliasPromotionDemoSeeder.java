package com.weaone.themoa.domain.merchant.support;

import com.weaone.themoa.domain.cardconnection.entity.Card;
import com.weaone.themoa.domain.cardconnection.entity.CardConnection;
import com.weaone.themoa.domain.cardconnection.entity.CardIssuer;
import com.weaone.themoa.domain.cardconnection.repository.CardConnectionRepository;
import com.weaone.themoa.domain.cardconnection.repository.CardIssuerRepository;
import com.weaone.themoa.domain.cardconnection.repository.CardRepository;
import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.category.repository.CategoryRepository;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpensePaymentMethod;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.merchant.entity.Merchant;
import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
import com.weaone.themoa.domain.merchant.entity.MerchantAliasTerms;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasTermsRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 관리자 "가맹점 & 서비스 마스터 관리" 화면의 전역 마스터 승격 대기목록(manage.html) 데모용 시드.
 * 회원 학습 표기(member_id NOT NULL)를 미리 심어 둬, F-05 미납 결제 확인 플로우를 직접 밟지 않아도
 * 앱 기동 직후 대기목록 화면에서 승격 동작을 바로 확인할 수 있게 한다.
 *
 * <p>정재훈({@code test1}, member_id=3)에게는 학습 이력이 전혀 없는 상태로 "쿠팡(주)정기결제"라는 원본
 * 가맹점명의 미분류 거래를 하나 심어 둔다 — 박아름·이신규가 학습한 바로 그 표기다. 승격 전에는 이 거래가
 * merchant_alias 없이 "미식별" 상태로 남아 있다가, 관리자가 승격 버튼을 누르는 순간
 * {@link com.weaone.themoa.domain.merchant.service.AdminMerchantService#promote}의 소급 재분류 로직이
 * 타서 그 자리에서 즉시 분류된다 — "승격 → 다른 회원 화면이 실제로 바뀐다"를 코드 변경 없이 바로 시연하기
 * 위한 장치다.
 *
 * <p>{@link com.weaone.themoa.domain.member.support.MemberDemoSeeder}(회원)·{@link MerchantAliasSeeder}
 * (전역 alias·카드사)가 먼저 끝나 있어야 하므로 그 사이 순번({@link Order})에 둔다.
 */
@Component
@Order(5)
@RequiredArgsConstructor
public class MerchantAliasPromotionDemoSeeder implements ApplicationRunner {

    private static final String MULTI_LEARNER_TEXT = "쿠팡(주)정기결제";
    private static final String SINGLE_LEARNER_TEXT = "대한적십자사(정기후원)";
    private static final String UNCLASSIFIED_APPROVAL_NO = "DEMO-PROMOTE-JAEHOON-001";
    private static final String MULTI_VARIANT_SERVICE = "ChatGPT 구독";
    private static final String[] MULTI_VARIANT_TEXTS = {"chat-g-p-t", "ChatGPT구독", "ai구독"};
    private static final String DUPLICATE_ALIAS_KO = "클로드구독";
    private static final String DUPLICATE_ALIAS_EN = "CLAUDE SUBSCRIPTION";

    private final MemberRepository memberRepository;
    private final MerchantAliasRepository merchantAliasRepository;
    private final MerchantAliasTermsRepository merchantAliasTermsRepository;
    private final MerchantRepository merchantRepository;
    private final CategoryRepository categoryRepository;
    private final CardIssuerRepository cardIssuerRepository;
    private final CardConnectionRepository cardConnectionRepository;
    private final CardRepository cardRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final FixedExpenseRepository fixedExpenseRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedMultiLearnerCandidate();
        seedSingleLearnerCandidate();
        seedMultiVariantCandidate();
        seedUnclassifiedBeneficiary();
        seedDuplicateServiceNames();
    }

    /** 학습자 2명 — "다수의 회원이 개별 학습" 문구가 가리키는 통상 케이스. */
    private void seedMultiLearnerCandidate() {
        Optional<MerchantAlias> aliasOpt = merchantAliasRepository.findByCanonicalServiceNameNormalized("쿠팡와우 멤버십");
        Optional<Member> areumOpt = memberRepository.findByEmail("test2");
        Optional<Member> newbieOpt = memberRepository.findByEmail("test3");
        if (aliasOpt.isEmpty() || areumOpt.isEmpty() || newbieOpt.isEmpty()) {
            return;
        }
        learnIfAbsent(aliasOpt.get(), areumOpt.get(), MULTI_LEARNER_TEXT);
        learnIfAbsent(aliasOpt.get(), newbieOpt.get(), MULTI_LEARNER_TEXT);
    }

    /** 학습자 1명 — 실제 쿼리엔 최소 인원 조건이 없어 1명만으로도 대기목록에 뜨는 경계 케이스. */
    private void seedSingleLearnerCandidate() {
        Optional<MerchantAlias> aliasOpt =
                merchantAliasRepository.findByCanonicalServiceNameNormalized("대한적십자사 정기후원");
        Optional<Member> newbieOpt = memberRepository.findByEmail("test3");
        if (aliasOpt.isEmpty() || newbieOpt.isEmpty()) {
            return;
        }
        learnIfAbsent(aliasOpt.get(), newbieOpt.get(), SINGLE_LEARNER_TEXT);
    }

    /**
     * 한 서비스에 서로 다른 원본 표기 3개가 각기 다른 회원에 의해 학습된 경우 — 관리자 화면에서
     * "chat-g-p-t" / "ChatGPT구독" / "ai구독"이 같은 서비스 카드 안에 표기 변형으로 묶여 보이는지
     * 확인하기 위한 시나리오다(단일 텍스트 반복 학습인 {@link #seedMultiLearnerCandidate}와는 다른 케이스).
     */
    private void seedMultiVariantCandidate() {
        Category subscription = categoryRepository.findByCode(CategoryCode.SUBSCRIPTION.name())
                .orElseThrow(() -> new IllegalStateException(CategoryCode.SUBSCRIPTION + " 카테고리가 시드되지 않았습니다."));
        MerchantAlias alias = merchantAliasRepository.findByCanonicalServiceNameNormalized(MULTI_VARIANT_SERVICE)
                .orElseGet(() -> merchantAliasRepository.save(MerchantAlias.create(MULTI_VARIANT_SERVICE, subscription)));

        Optional<Member> solminOpt = memberRepository.findByEmail("solmin");
        Optional<Member> areumOpt = memberRepository.findByEmail("test2");
        Optional<Member> newbieOpt = memberRepository.findByEmail("test3");
        if (solminOpt.isEmpty() || areumOpt.isEmpty() || newbieOpt.isEmpty()) {
            return;
        }
        learnIfAbsent(alias, solminOpt.get(), MULTI_VARIANT_TEXTS[0]);
        learnIfAbsent(alias, areumOpt.get(), MULTI_VARIANT_TEXTS[1]);
        learnIfAbsent(alias, newbieOpt.get(), MULTI_VARIANT_TEXTS[2]);
    }

    /**
     * 정재훈(member_id=3) — 이 표기를 학습한 적 없는 제3자. "쿠팡(주)정기결제"로 이미 결제내역이 쌓여 있지만
     * 전역 표기가 없어 미분류 상태다. 관리자 승격 전/후 화면 변화를 이 회원 기준으로 시연한다.
     */
    private void seedUnclassifiedBeneficiary() {
        Optional<Member> jaehoonOpt = memberRepository.findByEmail("test1");
        if (jaehoonOpt.isEmpty()) {
            return;
        }
        Member jaehoon = jaehoonOpt.get();
        if (cardConnectionRepository.findByMember_IdAndCardIssuer_Organization(jaehoon.getId(), "0302").isPresent()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        CardIssuer hyundai = cardIssuerRepository.findById("0302")
                .orElseThrow(() -> new IllegalStateException("현대카드(0302)가 시드되지 않았습니다."));
        CardConnection connection = cardConnectionRepository.save(
                CardConnection.connect(jaehoon, hyundai, "SEED-HYUNDAI-CONNECTED-JAEHOON-0001", now.minusMonths(2)));
        connection.markSynced(now.minusHours(3));
        Card card = cardRepository.save(
                Card.observe(jaehoon, connection, "현대카드 the Green", "5678-****-****-1234"));

        Category subscription = categoryRepository.findByCode(CategoryCode.SUBSCRIPTION.name())
                .orElseThrow(() -> new IllegalStateException(CategoryCode.SUBSCRIPTION + " 카테고리가 시드되지 않았습니다."));

        Merchant merchant = merchantRepository.findByMerchantNameRaw(MULTI_LEARNER_TEXT)
                .orElseGet(() -> merchantRepository.save(Merchant.observe(MULTI_LEARNER_TEXT, null)));

        LocalDate usedDate = LocalDate.now().minusDays(10);
        CardTransaction transaction = cardTransactionRepository.save(CardTransaction.sync(jaehoon, card, subscription,
                UNCLASSIFIED_APPROVAL_NO, usedDate, usedDate.atTime(9, 30), new BigDecimal("7890.00"),
                null, "KRW", null, false, TransactionStatus.APPROVED, null, false,
                MULTI_LEARNER_TEXT, null, null, null, null));
        transaction.assignMerchant(merchant, null);
    }

    /**
     * 실제 버그 재현: 고정지출을 등록할 때 기존 서비스를 검색해 고르지 않고 이름을 직접 입력하면, 이미 있는
     * "Claude 구독"(전역 시드)과는 별개인 새 {@link MerchantAlias}가 그대로 생겨버린다. 박아름·이신규가 각자
     * "클로드구독"/"CLAUDE SUBSCRIPTION"으로 따로 등록한 상황을 재현해, 관리자 "서비스 중복 탐지 & 병합"
     * 화면에서 실제로 병합해 볼 수 있게 한다.
     */
    private void seedDuplicateServiceNames() {
        if (merchantAliasRepository.findByCanonicalServiceNameNormalized(DUPLICATE_ALIAS_KO).isPresent()) {
            return;
        }
        Optional<Member> areumOpt = memberRepository.findByEmail("test2");
        Optional<Member> newbieOpt = memberRepository.findByEmail("test3");
        if (areumOpt.isEmpty() || newbieOpt.isEmpty()) {
            return;
        }
        Category subscription = categoryRepository.findByCode(CategoryCode.SUBSCRIPTION.name())
                .orElseThrow(() -> new IllegalStateException(CategoryCode.SUBSCRIPTION + " 카테고리가 시드되지 않았습니다."));

        MerchantAlias koAlias = merchantAliasRepository.save(MerchantAlias.create(DUPLICATE_ALIAS_KO, subscription));
        MerchantAlias enAlias = merchantAliasRepository.save(MerchantAlias.create(DUPLICATE_ALIAS_EN, subscription));

        fixedExpenseRepository.save(FixedExpense.registerDirect(areumOpt.get(), "클로드 구독료", subscription, koAlias,
                FixedExpensePaymentMethod.TRANSFER, (short) 15, new BigDecimal("22000.00"), "KRW",
                new BigDecimal("22000.00"), null, null));
        fixedExpenseRepository.save(FixedExpense.registerDirect(newbieOpt.get(), "Claude subscription", subscription,
                enAlias, FixedExpensePaymentMethod.TRANSFER, (short) 20, new BigDecimal("22000.00"), "KRW",
                new BigDecimal("22000.00"), null, null));
    }

    private void learnIfAbsent(MerchantAlias alias, Member member, String aliasText) {
        if (merchantAliasTermsRepository.findByMember_IdAndAliasText(member.getId(), aliasText).isPresent()) {
            return;
        }
        merchantAliasTermsRepository.save(MerchantAliasTerms.learn(alias, member, aliasText));
    }
}
