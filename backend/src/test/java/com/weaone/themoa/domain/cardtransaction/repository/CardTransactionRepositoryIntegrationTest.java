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
}
