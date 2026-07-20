package com.weaone.themoa.domain.cardtransaction.repository;

import com.weaone.themoa.domain.cardconnection.entity.Card;
import com.weaone.themoa.domain.cardconnection.entity.CardConnection;
import com.weaone.themoa.domain.cardconnection.entity.CardIssuer;
import com.weaone.themoa.domain.cardconnection.entity.CodefValueType;
import com.weaone.themoa.domain.cardconnection.repository.CardConnectionRepository;
import com.weaone.themoa.domain.cardconnection.repository.CardIssuerRepository;
import com.weaone.themoa.domain.cardconnection.repository.CardRepository;
import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import com.weaone.themoa.domain.cardtransaction.entity.PaymentMethod;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionSource;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.category.repository.CategoryRepository;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpensePaymentMethod;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseRepository;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.merchant.entity.Merchant;
import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DB UNIQUE 제약이 실제 MySQL에서 멱등성을 보장하는지 검증한다(cardtransaction.md §2, §7).
 * 테스트 DB는 실제 MySQL이며 각 테스트는 트랜잭션 롤백으로 격리된다(H2 미사용).
 */
@SpringBootTest
@Transactional
class CardTransactionRepositoryIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CardIssuerRepository cardIssuerRepository;
    @Autowired
    private CardConnectionRepository cardConnectionRepository;
    @Autowired
    private CardRepository cardRepository;
    @Autowired
    private CardTransactionRepository cardTransactionRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private FixedExpenseRepository fixedExpenseRepository;
    @Autowired
    private MerchantRepository merchantRepository;
    @Autowired
    private MerchantAliasRepository merchantAliasRepository;
    @Autowired
    private EntityManager entityManager;

    private Member persistMember(String email) {
        return memberRepository.save(
                Member.signUp(email, "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1), LocalDateTime.now()));
    }

    private CardConnection persistConnection(Member member) {
        CardIssuer issuer = cardIssuerRepository.findById("0306")
                .orElseThrow(() -> new IllegalStateException("신한카드 시드가 없습니다."));
        return cardConnectionRepository.save(CardConnection.connect(member, issuer, "connected-id", LocalDateTime.now()));
    }

    @Test
    @DisplayName("같은 커넥션에 같은 마스킹 카드번호를 두 번 저장하면 UNIQUE 위반이 난다 — card_id 안정성의 전제")
    void cardUniqueConstraintPreventsDuplicateCards() {
        Member member = persistMember("card-unique@example.com");
        CardConnection connection = persistConnection(member);
        cardRepository.saveAndFlush(Card.observe(member, connection, "카드", "4619****984*"));

        assertThatThrownBy(() ->
                cardRepository.saveAndFlush(Card.observe(member, connection, "카드", "4619****984*")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("복합 UNIQUE 키가 같은 거래 2건 저장을 막는다 — 재수집 멱등성")
    void cardTransactionUniqueConstraintPreventsDuplicateInsert() {
        Member member = persistMember("tx-unique@example.com");
        CardConnection connection = persistConnection(member);
        Card card = cardRepository.saveAndFlush(Card.observe(member, connection, "카드", "4619****984*"));
        Category category = categoryRepository.findByCode(CategoryCode.TRANSPORT.name())
                .orElseThrow(() -> new IllegalStateException("교통 카테고리 시드가 없습니다."));

        CardTransaction first = CardTransaction.sync(member, card, category, "43841056",
                LocalDate.of(2026, 6, 10), LocalDateTime.of(2026, 6, 10, 10, 30), BigDecimal.valueOf(9700),
                null, "KRW", null, false, TransactionStatus.APPROVED, null, false, "카카오T", "택시", null, null, null);
        cardTransactionRepository.saveAndFlush(first);

        CardTransaction duplicate = CardTransaction.sync(member, card, category, "43841056",
                LocalDate.of(2026, 6, 10), LocalDateTime.of(2026, 6, 10, 10, 30), BigDecimal.valueOf(9700),
                null, "KRW", null, false, TransactionStatus.APPROVED, null, false, "카카오T", "택시", null, null, null);

        assertThatThrownBy(() -> cardTransactionRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("승인번호만 다르면 같은 날짜·카드·시각이어도 별개 거래로 저장된다")
    void differentApprovalNoIsNotDuplicate() {
        Member member = persistMember("tx-diff-approval@example.com");
        CardConnection connection = persistConnection(member);
        Card card = cardRepository.saveAndFlush(Card.observe(member, connection, "카드", "4619****984*"));
        Category category = categoryRepository.findByCode(CategoryCode.TRANSPORT.name())
                .orElseThrow(() -> new IllegalStateException("교통 카테고리 시드가 없습니다."));

        CardTransaction first = CardTransaction.sync(member, card, category, "11111111",
                LocalDate.of(2026, 6, 10), LocalDateTime.of(2026, 6, 10, 10, 30), BigDecimal.valueOf(9700),
                null, "KRW", null, false, TransactionStatus.APPROVED, null, false, "카카오T", "택시", null, null, null);
        CardTransaction second = CardTransaction.sync(member, card, category, "22222222",
                LocalDate.of(2026, 6, 10), LocalDateTime.of(2026, 6, 10, 10, 30), BigDecimal.valueOf(9700),
                null, "KRW", null, false, TransactionStatus.APPROVED, null, false, "카카오T", "택시", null, null, null);

        cardTransactionRepository.saveAndFlush(first);
        cardTransactionRepository.saveAndFlush(second);

        assertThat(cardTransactionRepository.findAll()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("카테고리 집계는 고정지출 태그 거래를 제외하고, Type2 음수행은 순액 그대로 포함하며 0원 카테고리는 뺀다")
    void summarizeByCategoryExcludesFixedExpenseAndIncludesType2Row() {
        Member member = persistMember("cat-summary@example.com");
        CardConnection connection = persistConnection(member);
        Card card = cardRepository.saveAndFlush(Card.observe(member, connection, "카드", "4619****985*"));
        Category food = categoryRepository.findByCode(CategoryCode.FOOD.name())
                .orElseThrow(() -> new IllegalStateException("식비 카테고리 시드가 없습니다."));
        Category transport = categoryRepository.findByCode(CategoryCode.TRANSPORT.name())
                .orElseThrow(() -> new IllegalStateException("교통 카테고리 시드가 없습니다."));

        FixedExpense fixedExpense = fixedExpenseRepository.save(FixedExpense.registerDirect(member, "웨이브",
                food, null, FixedExpensePaymentMethod.TRANSFER, (short) 5, BigDecimal.valueOf(10900), "KRW",
                BigDecimal.valueOf(10900), null, null));

        // 일반 식비 거래(합산 대상)
        CardTransaction foodTx = CardTransaction.sync(member, card, food, "10000001",
                LocalDate.of(2026, 6, 5), LocalDateTime.of(2026, 6, 5, 12, 0), BigDecimal.valueOf(15000),
                null, "KRW", null, false, TransactionStatus.APPROVED, null, false, "김밥천국", "한식", null, null, null);
        cardTransactionRepository.saveAndFlush(foodTx);

        // 고정지출 태그 거래(집계 제외 대상)
        CardTransaction fixedTx = CardTransaction.sync(member, card, food, "10000002",
                LocalDate.of(2026, 6, 6), LocalDateTime.of(2026, 6, 6, 9, 0), BigDecimal.valueOf(10900),
                null, "KRW", null, false, TransactionStatus.APPROVED, null, false, "웨이브", "구독", null, null, null);
        fixedTx.assignFixedExpense(fixedExpense);
        cardTransactionRepository.saveAndFlush(fixedTx);

        // Type2 별도 음수 취소행(교통 카테고리). 음수행 자체의 순액은 음수라 도넛 합계·건수에서는
        // 빠지지만(category.md §6), canceledTotal에는 절대값으로 포함된다.
        CardTransaction type2ApprovedTx = CardTransaction.sync(member, card, transport, "10000003",
                LocalDate.of(2026, 6, 7), LocalDateTime.of(2026, 6, 7, 10, 0), BigDecimal.valueOf(50000),
                null, "KRW", null, false, TransactionStatus.APPROVED, null, false, "카카오T", "택시", null, null, null);
        CardTransaction type2CancelTx = CardTransaction.sync(member, card, transport, "10000004",
                LocalDate.of(2026, 6, 7), LocalDateTime.of(2026, 6, 7, 10, 30), BigDecimal.valueOf(-30000),
                null, "KRW", null, false, TransactionStatus.APPROVED, null, false, "카카오T", "택시", null, null, null);
        cardTransactionRepository.saveAndFlush(type2ApprovedTx);
        cardTransactionRepository.saveAndFlush(type2CancelTx);

        // 전액취소로 순액이 0원이 되는 카테고리(도넛에서 제외되어야 함)
        Category cafe = categoryRepository.findByCode(CategoryCode.CAFE.name())
                .orElseThrow(() -> new IllegalStateException("카페 카테고리 시드가 없습니다."));
        CardTransaction canceledTx = CardTransaction.sync(member, card, cafe, "10000005",
                LocalDate.of(2026, 6, 8), LocalDateTime.of(2026, 6, 8, 14, 0), BigDecimal.valueOf(5000),
                null, "KRW", null, false, TransactionStatus.CANCELED, BigDecimal.valueOf(5000), false,
                "스타벅스", "커피전문점", null, null, null);
        cardTransactionRepository.saveAndFlush(canceledTx);

        List<CardTransactionRepository.CategorySummary> summaries = cardTransactionRepository.summarizeByCategory(
                member.getId(), TransactionStatus.REJECTED, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
        BigDecimal canceledTotal = cardTransactionRepository.sumCanceledAmount(
                member.getId(), TransactionStatus.REJECTED, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(summaries).extracting(CardTransactionRepository.CategorySummary::getCategoryId)
                .containsExactlyInAnyOrder(food.getId(), transport.getId());

        CardTransactionRepository.CategorySummary foodSummary = summaries.stream()
                .filter(s -> s.getCategoryId().equals(food.getId())).findFirst().orElseThrow();
        assertThat(foodSummary.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(15000));
        assertThat(foodSummary.getTransactionCount()).isEqualTo(1);

        CardTransactionRepository.CategorySummary transportSummary = summaries.stream()
                .filter(s -> s.getCategoryId().equals(transport.getId())).findFirst().orElseThrow();
        assertThat(transportSummary.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(transportSummary.getTransactionCount()).isEqualTo(1);

        assertThat(canceledTotal).isEqualByComparingTo(BigDecimal.valueOf(35000));
    }

    @Test
    @DisplayName("대체된 수기 건은 @SQLRestriction으로 모든 조회에서 자동 제외된다 — 이중 계상 방지(entryMode.md §4-2)")
    void replacedManualEntryIsExcludedFromAllQueries() {
        Member member = persistMember("replace-restriction@example.com");
        Category food = categoryRepository.findByCode(CategoryCode.FOOD.name())
                .orElseThrow(() -> new IllegalStateException("식비 카테고리 시드가 없습니다."));

        CardTransaction manualEntry = CardTransaction.manual(member, food, PaymentMethod.CARD,
                LocalDate.of(2026, 6, 10), LocalDateTime.of(2026, 6, 10, 12, 0), BigDecimal.valueOf(9000),
                "카드결제", null);
        cardTransactionRepository.saveAndFlush(manualEntry);
        Long manualEntryId = manualEntry.getId();

        manualEntry.replace(null, LocalDateTime.now());
        cardTransactionRepository.saveAndFlush(manualEntry);
        // find-by-id는 1차 캐시를 먼저 보므로, 실제 SQL 재조회를 강제하려면 영속성 컨텍스트를 비워야 한다.
        entityManager.clear();

        assertThat(cardTransactionRepository.findByIdAndMember_Id(manualEntryId, member.getId())).isEmpty();
        BigDecimal netSpend = cardTransactionRepository.sumNetSpend(member.getId(), TransactionStatus.REJECTED,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
        assertThat(netSpend).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("대체 대상 조회는 아직 대체 안 된 갭 구간의 카드 태그 수기 건만 반환한다")
    void findsOnlyUnreplacedManualCardEntriesInWindow() {
        Member member = persistMember("replace-candidates@example.com");
        Category food = categoryRepository.findByCode(CategoryCode.FOOD.name())
                .orElseThrow(() -> new IllegalStateException("식비 카테고리 시드가 없습니다."));

        CardTransaction cardTagged = CardTransaction.manual(member, food, PaymentMethod.CARD,
                LocalDate.of(2026, 6, 10), LocalDateTime.of(2026, 6, 10, 12, 0), BigDecimal.valueOf(9000),
                "카드결제", null);
        CardTransaction cashTagged = CardTransaction.manual(member, food, PaymentMethod.CASH,
                LocalDate.of(2026, 6, 11), LocalDateTime.of(2026, 6, 11, 12, 0), BigDecimal.valueOf(5000),
                "현금결제", null);
        cardTransactionRepository.saveAndFlush(cardTagged);
        cardTransactionRepository.saveAndFlush(cashTagged);

        List<CardTransaction> candidates = cardTransactionRepository
                .findByMember_IdAndSourceAndPaymentMethodAndUsedDateBetween(member.getId(), TransactionSource.MANUAL,
                        PaymentMethod.CARD, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(candidates).extracting(CardTransaction::getId).containsExactly(cardTagged.getId());

        cardTagged.replace(null, LocalDateTime.now());
        cardTransactionRepository.saveAndFlush(cardTagged);
        entityManager.clear();

        List<CardTransaction> afterReplace = cardTransactionRepository
                .findByMember_IdAndSourceAndPaymentMethodAndUsedDateBetween(member.getId(), TransactionSource.MANUAL,
                        PaymentMethod.CARD, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
        assertThat(afterReplace).isEmpty();
    }

    @Test
    @DisplayName("대체 짝 탐색은 같은 날짜·금액의 카드 수집 거래를 정확 일치로 찾는다")
    void findsMatchingSyncTransactionByDateAndAmount() {
        Member member = persistMember("replace-match@example.com");
        CardConnection connection = persistConnection(member);
        Card card = cardRepository.saveAndFlush(Card.observe(member, connection, "카드", "4619****986*"));
        Category food = categoryRepository.findByCode(CategoryCode.FOOD.name())
                .orElseThrow(() -> new IllegalStateException("식비 카테고리 시드가 없습니다."));

        CardTransaction syncTx = CardTransaction.sync(member, card, food, "30000001",
                LocalDate.of(2026, 6, 10), LocalDateTime.of(2026, 6, 10, 12, 5), BigDecimal.valueOf(9000),
                null, "KRW", null, false, TransactionStatus.APPROVED, null, false, "김밥천국", "한식", null, null, null);
        cardTransactionRepository.saveAndFlush(syncTx);

        Optional<CardTransaction> match = cardTransactionRepository
                .findFirstByMember_IdAndSourceAndUsedDateAndAmountOrderByIdAsc(member.getId(), TransactionSource.SYNC,
                        LocalDate.of(2026, 6, 10), BigDecimal.valueOf(9000));

        assertThat(match).isPresent();
        assertThat(match.get().getId()).isEqualTo(syncTx.getId());
    }

    @Test
    @DisplayName("전체 소비내역 상세 결제내역 페이지는 usedAt DESC, id DESC로 정렬되고 고정지출·거절 거래를 제외한다")
    void findConsumptionHistoryPageOrdersAndFilters() {
        Member member = persistMember("history-page@example.com");
        CardConnection connection = persistConnection(member);
        Card card = cardRepository.saveAndFlush(Card.observe(member, connection, "카드", "4619****988*"));
        Category food = categoryRepository.findByCode(CategoryCode.FOOD.name())
                .orElseThrow(() -> new IllegalStateException("식비 카테고리 시드가 없습니다."));

        FixedExpense fixedExpense = fixedExpenseRepository.save(FixedExpense.registerDirect(member, "웨이브",
                food, null, FixedExpensePaymentMethod.TRANSFER, (short) 5, BigDecimal.valueOf(10900), "KRW",
                BigDecimal.valueOf(10900), null, null));

        CardTransaction earliest = CardTransaction.manual(member, food, PaymentMethod.CASH,
                LocalDate.of(2026, 7, 10), LocalDateTime.of(2026, 7, 10, 9, 0), BigDecimal.valueOf(5000), "김밥", null);
        CardTransaction sameInstantSmallerId = CardTransaction.manual(member, food, PaymentMethod.CASH,
                LocalDate.of(2026, 7, 10), LocalDateTime.of(2026, 7, 10, 12, 0), BigDecimal.valueOf(6000), "라면", null);
        CardTransaction sameInstantLargerId = CardTransaction.manual(member, food, PaymentMethod.CASH,
                LocalDate.of(2026, 7, 10), LocalDateTime.of(2026, 7, 10, 12, 0), BigDecimal.valueOf(7000), "국밥", null);
        CardTransaction latest = CardTransaction.manual(member, food, PaymentMethod.CASH,
                LocalDate.of(2026, 7, 11), LocalDateTime.of(2026, 7, 11, 9, 0), BigDecimal.valueOf(8000), "닭갈비", null);
        cardTransactionRepository.saveAndFlush(earliest);
        cardTransactionRepository.saveAndFlush(sameInstantSmallerId);
        cardTransactionRepository.saveAndFlush(sameInstantLargerId);
        cardTransactionRepository.saveAndFlush(latest);

        CardTransaction fixedTx = CardTransaction.manual(member, food, PaymentMethod.TRANSFER,
                LocalDate.of(2026, 7, 12), LocalDateTime.of(2026, 7, 12, 9, 0), BigDecimal.valueOf(10900), "웨이브", null);
        fixedTx.assignFixedExpense(fixedExpense);
        cardTransactionRepository.saveAndFlush(fixedTx);

        CardTransaction rejectedTx = CardTransaction.sync(member, card, food, "80000001",
                LocalDate.of(2026, 7, 13), LocalDateTime.of(2026, 7, 13, 9, 0), BigDecimal.valueOf(9000),
                null, "KRW", null, false, TransactionStatus.REJECTED, null, false,
                "거절가맹점", "한식", null, null, null);
        cardTransactionRepository.saveAndFlush(rejectedTx);

        entityManager.clear();

        Page<CardTransaction> page = cardTransactionRepository.findConsumptionHistoryPage(
                member.getId(), TransactionStatus.REJECTED, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(CardTransaction::getId)
                .containsExactly(latest.getId(), sameInstantLargerId.getId(), sameInstantSmallerId.getId(), earliest.getId());
        assertThat(page.getContent()).extracting(CardTransaction::getId)
                .doesNotContain(fixedTx.getId(), rejectedTx.getId());
    }

    @Test
    @DisplayName("많이 쓴 곳 TOP5는 alias > merchant > 수기 원본명 우선순위로 묶고, 순액 0 이하 그룹은 제외한다")
    void findMerchantTop5GroupsByPriorityAndExcludesNonPositive() {
        Member member = persistMember("merchant-top5@example.com");
        CardConnection connection = persistConnection(member);
        Card card = cardRepository.saveAndFlush(Card.observe(member, connection, "카드", "4619****987*"));
        Category cafe = categoryRepository.findByCode(CategoryCode.CAFE.name())
                .orElseThrow(() -> new IllegalStateException("카페 카테고리 시드가 없습니다."));

        MerchantAlias alias = merchantAliasRepository.save(MerchantAlias.create("스타벅스", cafe));
        Merchant merchantWithAlias = merchantRepository.save(Merchant.observe("스타벅스 강남R점", alias));
        Merchant merchantOnly = merchantRepository.save(Merchant.observe("이디야커피 역삼점", null));

        // alias 그룹: 정확히 위치가 잡힌 두 건 합산 48000원, 2건
        CardTransaction aliasTx1 = CardTransaction.sync(member, card, cafe, "70000001",
                LocalDate.of(2026, 7, 5), LocalDateTime.of(2026, 7, 5, 9, 0), BigDecimal.valueOf(20000),
                null, "KRW", null, false, TransactionStatus.APPROVED, null, false,
                "스타벅스 강남R점", "커피전문점", null, null, null);
        aliasTx1.assignMerchant(merchantWithAlias, alias);
        CardTransaction aliasTx2 = CardTransaction.sync(member, card, cafe, "70000002",
                LocalDate.of(2026, 7, 6), LocalDateTime.of(2026, 7, 6, 9, 0), BigDecimal.valueOf(28000),
                null, "KRW", null, false, TransactionStatus.APPROVED, null, false,
                "스타벅스 강남R점", "커피전문점", null, null, null);
        aliasTx2.assignMerchant(merchantWithAlias, alias);
        cardTransactionRepository.saveAndFlush(aliasTx1);
        cardTransactionRepository.saveAndFlush(aliasTx2);

        // merchant만 있는 그룹(alias 없음): 15000원, 1건
        CardTransaction merchantTx = CardTransaction.sync(member, card, cafe, "70000003",
                LocalDate.of(2026, 7, 7), LocalDateTime.of(2026, 7, 7, 9, 0), BigDecimal.valueOf(15000),
                null, "KRW", null, false, TransactionStatus.APPROVED, null, false,
                "이디야커피 역삼점", "커피전문점", null, null, null);
        merchantTx.assignMerchant(merchantOnly, null);
        cardTransactionRepository.saveAndFlush(merchantTx);

        // 수기 그룹(둘 다 없음, 대소문자/공백만 다름 → 같은 그룹): 3000 + 2000 = 5000원, 2건
        CardTransaction manualTx1 = CardTransaction.manual(member, cafe, PaymentMethod.CASH,
                LocalDate.of(2026, 7, 8), LocalDateTime.of(2026, 7, 8, 9, 0), BigDecimal.valueOf(3000), "동네카페", null);
        CardTransaction manualTx2 = CardTransaction.manual(member, cafe, PaymentMethod.CASH,
                LocalDate.of(2026, 7, 9), LocalDateTime.of(2026, 7, 9, 9, 0), BigDecimal.valueOf(2000), " 동네카페 ", null);
        cardTransactionRepository.saveAndFlush(manualTx1);
        cardTransactionRepository.saveAndFlush(manualTx2);

        // 전액취소로 순액 0 이하가 되는 그룹(TOP5에서 제외돼야 함)
        CardTransaction canceledOut = CardTransaction.sync(member, card, cafe, "70000004",
                LocalDate.of(2026, 7, 10), LocalDateTime.of(2026, 7, 10, 9, 0), BigDecimal.valueOf(4000),
                null, "KRW", null, false, TransactionStatus.CANCELED, BigDecimal.valueOf(4000), false,
                "빽다방 신논현점", "커피전문점", null, null, null);
        cardTransactionRepository.saveAndFlush(canceledOut);

        List<CardTransactionRepository.MerchantTop5Row> top5 = cardTransactionRepository.findMerchantTop5(
                member.getId(), TransactionStatus.REJECTED.name(), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertThat(top5).extracting(CardTransactionRepository.MerchantTop5Row::getMerchantKey)
                .containsExactly("ALIAS:" + alias.getId(), "MERCHANT:" + merchantOnly.getId(), "MANUAL:동네카페");

        CardTransactionRepository.MerchantTop5Row aliasRow = top5.get(0);
        assertThat(aliasRow.getDisplayName()).isEqualTo("스타벅스");
        assertThat(aliasRow.getNetAmount()).isEqualByComparingTo(BigDecimal.valueOf(48000));
        assertThat(aliasRow.getTransactionCount()).isEqualTo(2);

        CardTransactionRepository.MerchantTop5Row manualRow = top5.get(2);
        assertThat(manualRow.getNetAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(manualRow.getTransactionCount()).isEqualTo(2);

        assertThat(top5).extracting(CardTransactionRepository.MerchantTop5Row::getMerchantKey)
                .doesNotContain("MANUAL:빽다방 신논현점");
    }
}
