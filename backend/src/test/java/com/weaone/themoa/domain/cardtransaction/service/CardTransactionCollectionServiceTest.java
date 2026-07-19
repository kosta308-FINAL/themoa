package com.weaone.themoa.domain.cardtransaction.service;

import com.weaone.themoa.domain.cardconnection.entity.Card;
import com.weaone.themoa.domain.cardconnection.entity.CardConnection;
import com.weaone.themoa.domain.cardconnection.entity.CardIssuer;
import com.weaone.themoa.domain.cardconnection.entity.CodefValueType;
import com.weaone.themoa.domain.cardconnection.repository.CardRepository;
import com.weaone.themoa.domain.cardtransaction.client.CodefApprovalRecord;
import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.category.service.CategoryClassificationService;
import com.weaone.themoa.domain.fixedexpense.service.FixedExpenseMatchingService;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantRepository;
import com.weaone.themoa.domain.merchant.service.MerchantIdentityResult;
import com.weaone.themoa.domain.merchant.service.MerchantIdentityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CardTransactionCollectionServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long CARD_ID = 10L;
    private static final Long CONNECTION_ID = 100L;

    @Mock
    private CardRepository cardRepository;
    @Mock
    private CardTransactionRepository cardTransactionRepository;
    @Mock
    private MerchantIdentityService merchantIdentityService;
    @Mock
    private MerchantRepository merchantRepository;
    @Mock
    private MerchantAliasRepository merchantAliasRepository;
    @Mock
    private CategoryClassificationService categoryClassificationService;
    @Mock
    private ExchangeRateService exchangeRateService;
    @Mock
    private FixedExpenseMatchingService fixedExpenseMatchingService;

    @InjectMocks
    private CardTransactionCollectionService collectionService;

    private Member member() {
        Member member = Member.signUp("user@example.com", "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1), LocalDateTime.now());
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        return member;
    }

    private CardConnection connection(CardIssuer cardIssuer) {
        CardConnection connection = CardConnection.connect(member(), cardIssuer, "connected-id", LocalDateTime.now());
        ReflectionTestUtils.setField(connection, "id", CONNECTION_ID);
        return connection;
    }

    private Card card(CardConnection connection) {
        Card card = Card.observe(member(), connection, "카드", "4619****984*");
        ReflectionTestUtils.setField(card, "id", CARD_ID);
        return card;
    }

    private CardIssuer issuer(CodefValueType fxType, CodefValueType cancelType, boolean cancelAmountUncertain) {
        return CardIssuer.seed("0306", "신한카드", fxType, cancelType, cancelAmountUncertain, false);
    }

    private Category subscription() {
        Category category = Category.seed(CategoryCode.SUBSCRIPTION, "구독");
        ReflectionTestUtils.setField(category, "id", 7L);
        return category;
    }

    private CodefApprovalRecord domesticApproved() {
        return new CodefApprovalRecord("20260610", "103000", "4619****984*", "", "카카오T", "9700",
                "KRW", "43841056", "", "택시", "", "", "0", "", "", "");
    }

    @Test
    @DisplayName("처음 보는 국내 거래는 새로 저장한다")
    void createsNewDomesticTransaction() {
        CardIssuer cardIssuer = issuer(CodefValueType.TYPE1, CodefValueType.TYPE1, false);
        CardConnection connection = connection(cardIssuer);
        Card card = card(connection);
        given(cardRepository.findByCardConnection_IdAndCardNumberMasked(CONNECTION_ID, "4619****984*"))
                .willReturn(Optional.of(card));
        given(cardTransactionRepository
                .findByMember_IdAndCard_IdAndUsedDateAndUsedAtAndApprovalNo(any(), any(), any(), any(), any()))
                .willReturn(Optional.empty());
        given(merchantIdentityService.resolve(MEMBER_ID, "카카오T"))
                .willReturn(MerchantIdentityResult.identified(20L, 30L));
        given(categoryClassificationService.classify("카카오T", "택시")).willReturn(subscription());

        CollectionOutcome outcome = collectionService.collect(member(), connection, cardIssuer, domesticApproved());

        assertThat(outcome).isEqualTo(CollectionOutcome.CREATED);
        then(cardTransactionRepository).should().save(any(CardTransaction.class));
        then(exchangeRateService).should(never()).getRate(any(), any());
    }

    @Test
    @DisplayName("이미 저장된 거래(같은 멱등키)는 재저장하지 않고 갱신만 한다")
    void reconcilesExistingTransaction() {
        CardIssuer cardIssuer = issuer(CodefValueType.TYPE1, CodefValueType.TYPE1, false);
        CardConnection connection = connection(cardIssuer);
        Card card = card(connection);
        CardTransaction existing = CardTransaction.sync(member(), card, subscription(), "43841056",
                LocalDate.of(2026, 6, 10), LocalDateTime.of(2026, 6, 10, 10, 30), BigDecimal.valueOf(9700),
                null, "KRW", null, false, TransactionStatus.APPROVED, null, false, "카카오T", "택시", null, null, null);
        given(cardRepository.findByCardConnection_IdAndCardNumberMasked(CONNECTION_ID, "4619****984*"))
                .willReturn(Optional.of(card));
        given(cardTransactionRepository
                .findByMember_IdAndCard_IdAndUsedDateAndUsedAtAndApprovalNo(any(), any(), any(), any(), any()))
                .willReturn(Optional.of(existing));
        given(merchantIdentityService.resolve(MEMBER_ID, "카카오T"))
                .willReturn(MerchantIdentityResult.identified(20L, 30L));

        CollectionOutcome outcome = collectionService.collect(member(), connection, cardIssuer, domesticApproved());

        assertThat(outcome).isEqualTo(CollectionOutcome.UPDATED);
        then(cardTransactionRepository).should(never()).save(any());
        then(categoryClassificationService).should(never()).classify(any(), any());
    }

    @Test
    @DisplayName("전체취소(resCancelYN=1)는 취소금액이 amount 전액이 되어 실지출이 0이 된다")
    void fullCancellationZeroesNetAmount() {
        CardIssuer cardIssuer = issuer(CodefValueType.TYPE1, CodefValueType.TYPE1, false);
        CardConnection connection = connection(cardIssuer);
        Card card = card(connection);
        CodefApprovalRecord canceled = new CodefApprovalRecord("20260610", "143200", "4619****984*", "",
                "카카오T시스템_결제", "9700", "KRW", "39259231", "", "택시", "", "", "1", "9700", "", "");
        given(cardRepository.findByCardConnection_IdAndCardNumberMasked(CONNECTION_ID, "4619****984*"))
                .willReturn(Optional.of(card));
        given(cardTransactionRepository
                .findByMember_IdAndCard_IdAndUsedDateAndUsedAtAndApprovalNo(any(), any(), any(), any(), any()))
                .willReturn(Optional.empty());
        given(merchantIdentityService.resolve(any(), any())).willReturn(MerchantIdentityResult.identified(20L, 30L));
        given(categoryClassificationService.classify(any(), any())).willReturn(subscription());

        collectionService.collect(member(), connection, cardIssuer, canceled);

        var captor = org.mockito.ArgumentCaptor.forClass(CardTransaction.class);
        then(cardTransactionRepository).should().save(captor.capture());
        CardTransaction saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.CANCELED);
        assertThat(saved.getNetAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Type 2 별도 음수 취소행은 status=CANCELED여도 canceled_amount를 채우지 않고 순액이 음수 그대로 유지된다")
    void type2CancellationRowKeepsNegativeNetAmount() {
        CardIssuer cardIssuer = issuer(CodefValueType.TYPE1, CodefValueType.TYPE1, false);
        CardConnection connection = connection(cardIssuer);
        Card card = card(connection);
        CodefApprovalRecord type2CancelRow = new CodefApprovalRecord("20260610", "143500", "4619****984*", "",
                "가맹점", "-30000", "KRW", "39259232", "", "택시", "", "", "1", "", "", "");
        given(cardRepository.findByCardConnection_IdAndCardNumberMasked(CONNECTION_ID, "4619****984*"))
                .willReturn(Optional.of(card));
        given(cardTransactionRepository
                .findByMember_IdAndCard_IdAndUsedDateAndUsedAtAndApprovalNo(any(), any(), any(), any(), any()))
                .willReturn(Optional.empty());
        given(merchantIdentityService.resolve(any(), any())).willReturn(MerchantIdentityResult.identified(20L, 30L));
        given(categoryClassificationService.classify(any(), any())).willReturn(subscription());

        collectionService.collect(member(), connection, cardIssuer, type2CancelRow);

        var captor = org.mockito.ArgumentCaptor.forClass(CardTransaction.class);
        then(cardTransactionRepository).should().save(captor.capture());
        CardTransaction saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.CANCELED);
        assertThat(saved.getCanceledAmount()).isNull();
        assertThat(saved.getNetAmount()).isEqualByComparingTo("-30000");
    }

    @Test
    @DisplayName("부분취소 금액이 부정확한 카드사는 취소금액을 비우고 uncertain 플래그만 세운다")
    void partialCancelWithUncertainIssuer() {
        CardIssuer cardIssuer = issuer(CodefValueType.TYPE1, CodefValueType.TYPE2, true);
        CardConnection connection = connection(cardIssuer);
        Card card = card(connection);
        CodefApprovalRecord partial = new CodefApprovalRecord("20260610", "103000", "4619****984*", "",
                "가맹점", "50000", "KRW", "11112222", "", "", "", "", "2", "10000", "", "");
        given(cardRepository.findByCardConnection_IdAndCardNumberMasked(CONNECTION_ID, "4619****984*"))
                .willReturn(Optional.of(card));
        given(cardTransactionRepository
                .findByMember_IdAndCard_IdAndUsedDateAndUsedAtAndApprovalNo(any(), any(), any(), any(), any()))
                .willReturn(Optional.empty());
        given(merchantIdentityService.resolve(any(), any())).willReturn(MerchantIdentityResult.identified(20L, null));
        given(categoryClassificationService.classify(any(), any())).willReturn(subscription());

        collectionService.collect(member(), connection, cardIssuer, partial);

        var captor = org.mockito.ArgumentCaptor.forClass(CardTransaction.class);
        then(cardTransactionRepository).should().save(captor.capture());
        CardTransaction saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.PARTIAL_CANCELED);
        assertThat(saved.getCanceledAmount()).isNull();
        assertThat(saved.isCancelAmountUncertain()).isTrue();
        assertThat(saved.getNetAmount()).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("롯데카드는 국내 부분취소면 정확한 취소금액을 그대로 쓴다(해외 건만 불확실)")
    void lotteDomesticPartialCancelUsesProvidedAmount() {
        CardIssuer lotte = CardIssuer.seed("0311", "롯데카드", CodefValueType.TYPE2, CodefValueType.TYPE1, true, true);
        CardConnection connection = connection(lotte);
        Card card = card(connection);
        CodefApprovalRecord domesticPartial = new CodefApprovalRecord("20260610", "103000", "4619****984*", "",
                "가맹점", "50000", "KRW", "11112222", "", "", "", "", "2", "10000", "", "");
        given(cardRepository.findByCardConnection_IdAndCardNumberMasked(CONNECTION_ID, "4619****984*"))
                .willReturn(Optional.of(card));
        given(cardTransactionRepository
                .findByMember_IdAndCard_IdAndUsedDateAndUsedAtAndApprovalNo(any(), any(), any(), any(), any()))
                .willReturn(Optional.empty());
        given(merchantIdentityService.resolve(any(), any())).willReturn(MerchantIdentityResult.identified(20L, null));
        given(categoryClassificationService.classify(any(), any())).willReturn(subscription());

        collectionService.collect(member(), connection, lotte, domesticPartial);

        var captor = org.mockito.ArgumentCaptor.forClass(CardTransaction.class);
        then(cardTransactionRepository).should().save(captor.capture());
        CardTransaction saved = captor.getValue();
        assertThat(saved.getCanceledAmount()).isEqualByComparingTo("10000");
        assertThat(saved.isCancelAmountUncertain()).isFalse();
    }

    @Test
    @DisplayName("롯데카드는 해외 부분취소면 취소금액이 불확실하다")
    void lotteForeignPartialCancelIsUncertain() {
        CardIssuer lotte = CardIssuer.seed("0311", "롯데카드", CodefValueType.TYPE2, CodefValueType.TYPE1, true, true);
        CardConnection connection = connection(lotte);
        Card card = card(connection);
        CodefApprovalRecord foreignPartial = new CodefApprovalRecord("20260610", "103000", "4619****984*", "",
                "가맹점", "50000", "USD", "11112222", "2", "", "", "", "2", "10000", "", "");
        given(cardRepository.findByCardConnection_IdAndCardNumberMasked(CONNECTION_ID, "4619****984*"))
                .willReturn(Optional.of(card));
        given(cardTransactionRepository
                .findByMember_IdAndCard_IdAndUsedDateAndUsedAtAndApprovalNo(any(), any(), any(), any(), any()))
                .willReturn(Optional.empty());
        given(merchantIdentityService.resolve(any(), any())).willReturn(MerchantIdentityResult.identified(20L, null));
        given(categoryClassificationService.classify(any(), any())).willReturn(subscription());

        collectionService.collect(member(), connection, lotte, foreignPartial);

        var captor = org.mockito.ArgumentCaptor.forClass(CardTransaction.class);
        then(cardTransactionRepository).should().save(captor.capture());
        CardTransaction saved = captor.getValue();
        assertThat(saved.getCanceledAmount()).isNull();
        assertThat(saved.isCancelAmountUncertain()).isTrue();
    }

    @Test
    @DisplayName("type2 카드는 해외결제여도 환산하지 않고 resUsedAmount를 그대로 저장한다")
    void type2CardSkipsConversion() {
        CardIssuer cardIssuer = issuer(CodefValueType.TYPE2, CodefValueType.TYPE1, false);
        CardConnection connection = connection(cardIssuer);
        Card card = card(connection);
        CodefApprovalRecord foreign = new CodefApprovalRecord("20260610", "103000", "4619****984*", "",
                "ANTHROPIC* CLAUDE SUB", "29500", "USD", "590688", "2", "", "", "", "0", "", "", "");
        given(cardRepository.findByCardConnection_IdAndCardNumberMasked(CONNECTION_ID, "4619****984*"))
                .willReturn(Optional.of(card));
        given(cardTransactionRepository
                .findByMember_IdAndCard_IdAndUsedDateAndUsedAtAndApprovalNo(any(), any(), any(), any(), any()))
                .willReturn(Optional.empty());
        given(merchantIdentityService.resolve(any(), any())).willReturn(MerchantIdentityResult.identified(20L, 30L));
        given(categoryClassificationService.classify(any(), any())).willReturn(subscription());

        collectionService.collect(member(), connection, cardIssuer, foreign);

        var captor = org.mockito.ArgumentCaptor.forClass(CardTransaction.class);
        then(cardTransactionRepository).should().save(captor.capture());
        CardTransaction saved = captor.getValue();
        assertThat(saved.getAmount()).isEqualByComparingTo("29500");
        assertThat(saved.getOriginalAmount()).isNull();
        then(exchangeRateService).should(never()).getRate(any(), any());
    }

    @Test
    @DisplayName("type1 카드는 resKRWAmt가 있으면 역산 환율로 저장하고 환율 API를 호출하지 않는다")
    void type1CardUsesProvidedKrwAmount() {
        CardIssuer cardIssuer = issuer(CodefValueType.TYPE1, CodefValueType.TYPE1, false);
        CardConnection connection = connection(cardIssuer);
        Card card = card(connection);
        CodefApprovalRecord foreign = new CodefApprovalRecord("20260610", "103000", "4619****984*", "",
                "ANTHROPIC* CLAUDE SUB", "22.00", "USD", "590688", "2", "", "", "", "0", "", "30300", "");
        given(cardRepository.findByCardConnection_IdAndCardNumberMasked(CONNECTION_ID, "4619****984*"))
                .willReturn(Optional.of(card));
        given(cardTransactionRepository
                .findByMember_IdAndCard_IdAndUsedDateAndUsedAtAndApprovalNo(any(), any(), any(), any(), any()))
                .willReturn(Optional.empty());
        given(merchantIdentityService.resolve(any(), any())).willReturn(MerchantIdentityResult.identified(20L, 30L));
        given(categoryClassificationService.classify(any(), any())).willReturn(subscription());

        collectionService.collect(member(), connection, cardIssuer, foreign);

        var captor = org.mockito.ArgumentCaptor.forClass(CardTransaction.class);
        then(cardTransactionRepository).should().save(captor.capture());
        CardTransaction saved = captor.getValue();
        assertThat(saved.getAmount()).isEqualByComparingTo("30300");
        assertThat(saved.getOriginalAmount()).isEqualByComparingTo("22.00");
        then(exchangeRateService).should(never()).getRate(any(), any());
    }

    @Test
    @DisplayName("type1 카드는 resKRWAmt가 없으면 환율 API를 호출해 환산한다")
    void type1CardCallsExchangeRateApiWhenKrwAmountMissing() {
        CardIssuer cardIssuer = issuer(CodefValueType.TYPE1, CodefValueType.TYPE1, false);
        CardConnection connection = connection(cardIssuer);
        Card card = card(connection);
        CodefApprovalRecord foreign = new CodefApprovalRecord("20260610", "103000", "4619****984*", "",
                "ANTHROPIC* CLAUDE SUB", "22.00", "USD", "590688", "2", "", "", "", "0", "", "", "");
        given(cardRepository.findByCardConnection_IdAndCardNumberMasked(CONNECTION_ID, "4619****984*"))
                .willReturn(Optional.of(card));
        given(cardTransactionRepository
                .findByMember_IdAndCard_IdAndUsedDateAndUsedAtAndApprovalNo(any(), any(), any(), any(), any()))
                .willReturn(Optional.empty());
        given(merchantIdentityService.resolve(any(), any())).willReturn(MerchantIdentityResult.identified(20L, 30L));
        given(categoryClassificationService.classify(any(), any())).willReturn(subscription());
        given(exchangeRateService.getRate("USD", LocalDate.of(2026, 6, 10)))
                .willReturn(new ExchangeRateResult(BigDecimal.valueOf(1392.40), false));

        collectionService.collect(member(), connection, cardIssuer, foreign);

        var captor = org.mockito.ArgumentCaptor.forClass(CardTransaction.class);
        then(cardTransactionRepository).should().save(captor.capture());
        CardTransaction saved = captor.getValue();
        assertThat(saved.getAmount()).isEqualByComparingTo("30632.80");
        assertThat(saved.isExchangeRateEstimated()).isFalse();
    }

    @Test
    @DisplayName("환율을 전혀 구할 수 없으면 이 거래만 건너뛰고 저장하지 않는다")
    void skipsWhenExchangeRateUnavailable() {
        CardIssuer cardIssuer = issuer(CodefValueType.TYPE1, CodefValueType.TYPE1, false);
        CardConnection connection = connection(cardIssuer);
        Card card = card(connection);
        CodefApprovalRecord foreign = new CodefApprovalRecord("20260610", "103000", "4619****984*", "",
                "ANTHROPIC* CLAUDE SUB", "22.00", "USD", "590688", "2", "", "", "", "0", "", "", "");
        given(cardRepository.findByCardConnection_IdAndCardNumberMasked(CONNECTION_ID, "4619****984*"))
                .willReturn(Optional.of(card));
        given(cardTransactionRepository
                .findByMember_IdAndCard_IdAndUsedDateAndUsedAtAndApprovalNo(any(), any(), any(), any(), any()))
                .willReturn(Optional.empty());
        given(exchangeRateService.getRate(any(), any()))
                .willThrow(new ExchangeRateUnavailableException("환율 없음"));

        CollectionOutcome outcome = collectionService.collect(member(), connection, cardIssuer, foreign);

        assertThat(outcome).isEqualTo(CollectionOutcome.SKIPPED);
        then(cardTransactionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("resCancelYN 값이 알 수 없는 값이면 억지 추측 없이 예외를 던진다")
    void rejectsUnknownCancelYn() {
        CardIssuer cardIssuer = issuer(CodefValueType.TYPE1, CodefValueType.TYPE1, false);
        CardConnection connection = connection(cardIssuer);
        Card card = card(connection);
        CodefApprovalRecord invalid = new CodefApprovalRecord("20260610", "103000", "4619****984*", "",
                "가맹점", "1000", "KRW", "11112222", "", "", "", "", "9", "", "", "");
        given(cardRepository.findByCardConnection_IdAndCardNumberMasked(CONNECTION_ID, "4619****984*"))
                .willReturn(Optional.of(card));

        assertThatThrownBy(() -> collectionService.collect(member(), connection, cardIssuer, invalid))
                .isInstanceOf(IllegalStateException.class);
    }
}
