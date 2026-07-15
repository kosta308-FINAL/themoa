package com.weaone.themoa.domain.cardtransaction.repository;

import com.weaone.themoa.domain.cardconnection.entity.Card;
import com.weaone.themoa.domain.cardconnection.entity.CardConnection;
import com.weaone.themoa.domain.cardconnection.entity.CardIssuer;
import com.weaone.themoa.domain.cardconnection.entity.CodefValueType;
import com.weaone.themoa.domain.cardconnection.repository.CardConnectionRepository;
import com.weaone.themoa.domain.cardconnection.repository.CardIssuerRepository;
import com.weaone.themoa.domain.cardconnection.repository.CardRepository;
import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    private Member persistMember(String email) {
        return memberRepository.save(
                Member.signUp(email, "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1)));
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
}
