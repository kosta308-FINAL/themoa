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
 * <p>정재훈({@code test1}, member_id=3)에게는 승격 대기목록에 뜨는 표기를 하나도 학습한 적 없는 제3자
 * 입장에서 쿠팡·ANTHROPIC* CLAUDE JUL로 이미 결제내역이 쌓여 있게 심어 둔다. 승격 전에는 이 거래들이
 * merchant_alias 없이 "미식별" 상태로 남아 있다가, 관리자가 각 카드의 승격 버튼을 누르는 순간
 * {@link com.weaone.themoa.domain.merchant.service.AdminMerchantService#promote}의 소급 재분류 로직이
 * 타서 그 자리에서 즉시 분류된다.
 *
 * <p>{@link com.weaone.themoa.domain.member.support.MemberDemoSeeder}(회원)·{@link MerchantAliasSeeder}
 * (전역 alias·카드사)가 먼저 끝나 있어야 하므로 그 사이 순번({@link Order})에 둔다.
 */
@Component
@Order(5)
@RequiredArgsConstructor
public class MerchantAliasPromotionDemoSeeder implements ApplicationRunner {

    /** plan/output/codefapiResponse.json 실제 CODEF 응답에서 확인한 쿠팡 결제 표기 중 하나("쿠팡"/
     * "쿠팡이츠"/"쿠팡(쿠페이)"). "쿠팡(쿠페이)"는 {@link MerchantAliasSeeder}가 이미 전역으로 심어놔서
     * 그걸 쓰면 대기 상태를 재현할 수 없어, 아직 전역 미등록인 "쿠팡"을 쓴다. */
    private static final String MULTI_LEARNER_TEXT = "쿠팡";
    private static final String DUPLICATE_ALIAS_KO = "클로드구독";
    private static final String DUPLICATE_ALIAS_EN = "CLAUDE SUBSCRIPTION";
    /**
     * codefapiResponse.json엔 Claude 결제가 "ANTHROPIC* CLAUDE SUB"/"CLAUDE.AI SUBSCRIPTION" 두 형태로
     * 찍혀 있는데, 이미 둘 다 {@link MerchantAliasSeeder}가 전역으로 심어놔서 그대로 쓰면 대기 상태를
     * 재현할 수 없다. 그래서 같은 "ANTHROPIC* CLAUDE ___" 규칙을 따르는 세 번째 변형(청구월 라벨이 다른
     * 경우를 가정)을 만들어 썼다 — 이 정확한 문자열 자체가 실제로 관측된 건 아니다.
     * {@link com.weaone.themoa.domain.fixedexpense.support.FixedExpenseHabitDemoDataSeeder}에서 solmin이
     * 개인적으로 학습해 둔 바로 그 원본 표기이기도 하다.
     */
    private static final String CLAUDE_TEXT = "ANTHROPIC* CLAUDE JUL";
    /**
     * F-03 검색 드롭다운({@link com.weaone.themoa.domain.merchant.repository.MerchantAliasRepository
     * #findByCanonicalServiceNameContainingIgnoreCaseOrderByCanonicalServiceNameAsc}, 부분일치 LIKE)
     * 오염 재현용 — merchant_alias_terms가 하나도 없는(raw text 연결이 전혀 없는), 이름만 다른 중복
     * 서비스 2개. 둘 다 "멜론"을 포함해야 검색 한 번에 같이 뜬다(부분일치라서 "melon"처럼 영문으로만
     * 쓰면 "멜론"으로 검색했을 때 안 걸림). Claude 트리오와 안 겹치게 새 서비스로 만든다. solmin이
     * "고정지출 등록 → 카드형 → 서비스 검색"에서 "멜론"으로 검색하면 이 둘이 같이 뜨는 걸로, "비슷한
     * 이름 여러 개 중 하나를 잘못 고르거나 또 새로 만들어버릴 수 있다"는 위험을 solmin 계정에서
     * 직접 테스트할 수 있게 한다.
     */
    private static final String DUPLICATE_ALIAS_MELON_KO = "멜론 정기결제";
    private static final String DUPLICATE_ALIAS_MELON_VARIANT = "멜론뮤직 구독";

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
        seedUnclassifiedBeneficiary();
        seedDuplicateServiceNames();
        seedSearchPickerDuplicateDemo();
    }

    /** 학습자 2명, 제안된 서비스명은 1개뿐인 통상 케이스 — 충돌 케이스와 대조하기 위한 정상 대조군. */
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

    /**
     * 정재훈(member_id=3) — 승격 대기목록에 뜨는 표기를 하나도 학습한 적 없는 제3자다. 쿠팡·
     * ANTHROPIC* CLAUDE JUL로 이미 결제내역이 쌓여 있지만 전역 표기가 없어 미분류 상태다. 관리자가
     * 카드를 승격할 때마다 정재훈 화면에서 그 거래만 실시간으로 분류되는 걸 보여준다.
     */
    private void seedUnclassifiedBeneficiary() {
        Category subscription = categoryRepository.findByCode(CategoryCode.SUBSCRIPTION.name())
                .orElseThrow(() -> new IllegalStateException(CategoryCode.SUBSCRIPTION + " 카테고리가 시드되지 않았습니다."));

        memberRepository.findByEmail("test1").ifPresent(jaehoon -> {
            Card card = ensureDemoCard(jaehoon, "SEED-HYUNDAI-CONNECTED-JAEHOON-0001", "5678-****-****-1234");
            seedUnclassifiedTransaction(jaehoon, card, subscription, MULTI_LEARNER_TEXT,
                    "DEMO-PROMOTE-JAEHOON-001", new BigDecimal("7890.00"), 10);
            seedUnclassifiedTransaction(jaehoon, card, subscription, CLAUDE_TEXT,
                    "DEMO-PROMOTE-JAEHOON-002", new BigDecimal("22000.00"), 6);
        });
    }

    /** 데모용 카드 1장을 회원별로 하나씩 마련한다 — 이미 있으면 재사용한다(재기동 대비). */
    private Card ensureDemoCard(Member member, String connectedId, String cardNumberMasked) {
        CardConnection connection = cardConnectionRepository
                .findByMember_IdAndCardIssuer_Organization(member.getId(), "0302")
                .orElseGet(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    CardIssuer hyundai = cardIssuerRepository.findById("0302")
                            .orElseThrow(() -> new IllegalStateException("현대카드(0302)가 시드되지 않았습니다."));
                    CardConnection created = cardConnectionRepository.save(CardConnection.connect(
                            member, hyundai, connectedId, now.minusMonths(2)));
                    created.markSynced(now.minusHours(3));
                    return created;
                });
        return cardRepository.findByCardConnection_IdAndCardNumberMasked(connection.getId(), cardNumberMasked)
                .orElseGet(() -> cardRepository.save(
                        Card.observe(member, connection, "현대카드 the Green", cardNumberMasked)));
    }

    /** 미분류 거래를 하나 심는다 — 이미 같은 승인번호로 심어져 있으면 건너뛴다(재기동 대비). */
    private void seedUnclassifiedTransaction(Member member, Card card, Category category, String merchantNameRaw,
                                              String approvalNo, BigDecimal amount, int daysAgo) {
        if (cardTransactionRepository.existsByApprovalNo(approvalNo)) {
            return;
        }
        Merchant merchant = merchantRepository.findByMerchantNameRaw(merchantNameRaw)
                .orElseGet(() -> merchantRepository.save(Merchant.observe(merchantNameRaw, null)));

        LocalDate usedDate = LocalDate.now().minusDays(daysAgo);
        CardTransaction transaction = cardTransactionRepository.save(CardTransaction.sync(member, card, category,
                approvalNo, usedDate, usedDate.atTime(9, 30), amount,
                null, "KRW", null, false, TransactionStatus.APPROVED, null, false,
                merchantNameRaw, null, null, null, null));
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

    /**
     * F-03 검색 드롭다운 오염 재현. 박아름·이신규가 "멜론" 구독을 각자 직접 등록(검색 안 하고 이름
     * 타이핑)해서 raw text 연결이 하나도 없는 중복 서비스 2개가 이미 있는 상태를 만든다. solmin이
     * 고정지출을 새로 등록하면서 "멜론"으로 검색하면 이 두 개가 나란히 뜨는 걸 직접 볼 수 있다.
     */
    private void seedSearchPickerDuplicateDemo() {
        if (merchantAliasRepository.findByCanonicalServiceNameNormalized(DUPLICATE_ALIAS_MELON_KO).isPresent()) {
            return;
        }
        Optional<Member> areumOpt = memberRepository.findByEmail("test2");
        Optional<Member> newbieOpt = memberRepository.findByEmail("test3");
        if (areumOpt.isEmpty() || newbieOpt.isEmpty()) {
            return;
        }
        Category subscription = categoryRepository.findByCode(CategoryCode.SUBSCRIPTION.name())
                .orElseThrow(() -> new IllegalStateException(CategoryCode.SUBSCRIPTION + " 카테고리가 시드되지 않았습니다."));

        MerchantAlias koAlias = merchantAliasRepository.save(MerchantAlias.create(DUPLICATE_ALIAS_MELON_KO, subscription));
        MerchantAlias variantAlias = merchantAliasRepository.save(
                MerchantAlias.create(DUPLICATE_ALIAS_MELON_VARIANT, subscription));

        fixedExpenseRepository.save(FixedExpense.registerDirect(areumOpt.get(), "멜론 이용료", subscription, koAlias,
                FixedExpensePaymentMethod.TRANSFER, (short) 10, new BigDecimal("10900.00"), "KRW",
                new BigDecimal("10900.00"), null, null));
        fixedExpenseRepository.save(FixedExpense.registerDirect(newbieOpt.get(), "멜론뮤직 이용료", subscription,
                variantAlias, FixedExpensePaymentMethod.TRANSFER, (short) 25, new BigDecimal("10900.00"), "KRW",
                new BigDecimal("10900.00"), null, null));
    }

    private void learnIfAbsent(MerchantAlias alias, Member member, String aliasText) {
        if (merchantAliasTermsRepository.findByMember_IdAndAliasText(member.getId(), aliasText).isPresent()) {
            return;
        }
        merchantAliasTermsRepository.save(MerchantAliasTerms.learn(alias, member, aliasText));
    }
}
