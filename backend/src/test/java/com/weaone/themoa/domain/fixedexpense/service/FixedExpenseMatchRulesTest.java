package com.weaone.themoa.domain.fixedexpense.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** fixedExpense.md §5 수집 매칭 조건②·③의 순수 판정 로직 검증. */
class FixedExpenseMatchRulesTest {

    @Test
    void 국내_금액이_정확히_일치하면_매칭된다() {
        boolean result = FixedExpenseMatchRules.isAmountMatch("KRW", new BigDecimal("6300"), new BigDecimal("6300"),
                "KRW", new BigDecimal("6300"), null);

        assertThat(result).isTrue();
    }

    @Test
    void 국내_금액이_허용오차_10퍼센트_경계값이면_매칭된다() {
        // 기대금액 10,000원 → 허용 상한 11,000원(±10%)
        boolean result = FixedExpenseMatchRules.isAmountMatch("KRW", new BigDecimal("10000"), new BigDecimal("10000"),
                "KRW", new BigDecimal("11000"), null);

        assertThat(result).isTrue();
    }

    @Test
    void 국내_금액이_허용오차_10퍼센트를_초과하면_매칭되지_않는다() {
        boolean result = FixedExpenseMatchRules.isAmountMatch("KRW", new BigDecimal("10000"), new BigDecimal("10000"),
                "KRW", new BigDecimal("11001"), null);

        assertThat(result).isFalse();
    }

    @Test
    void 해외_type1은_외화_원금이_정확히_일치해야_매칭된다() {
        // CLAUDE.AI SUBSCRIPTION $22.00 실데이터 사례(fixedExpense.md §2)
        boolean result = FixedExpenseMatchRules.isAmountMatch("USD", new BigDecimal("22.00"), new BigDecimal("30000"),
                "USD", new BigDecimal("29500"), new BigDecimal("22.00"));

        assertThat(result).isTrue();
    }

    @Test
    void 해외_type1은_원화_환산액이_아니라_외화_원금으로_비교하므로_환율이_흔들려도_매칭된다() {
        // 원화 환산액(amount)은 매달 다르지만 외화 원금(originalAmount)만 같으면 매칭돼야 한다.
        boolean result = FixedExpenseMatchRules.isAmountMatch("USD", new BigDecimal("22.00"), new BigDecimal("30000"),
                "USD", new BigDecimal("31200"), new BigDecimal("22.00"));

        assertThat(result).isTrue();
    }

    @Test
    void 해외_type1은_외화_원금이_달라지면_가격_인상으로_보고_매칭되지_않는다() {
        boolean result = FixedExpenseMatchRules.isAmountMatch("USD", new BigDecimal("22.00"), new BigDecimal("30000"),
                "USD", new BigDecimal("34000"), new BigDecimal("25.00"));

        assertThat(result).isFalse();
    }

    @Test
    void 해외_type2는_외화_원금이_없어_원화_15퍼센트_오차로_폴백한다() {
        // fx_type=TYPE2 카드사는 originalAmount가 항상 NULL이다(cardtransaction.md §4).
        boolean result = FixedExpenseMatchRules.isAmountMatch("USD", new BigDecimal("22.00"), new BigDecimal("30000"),
                "KRW", new BigDecimal("34000"), null);

        assertThat(result).isTrue();
    }

    @Test
    void 해외_type2는_원화_15퍼센트_오차를_초과하면_매칭되지_않는다() {
        boolean result = FixedExpenseMatchRules.isAmountMatch("USD", new BigDecimal("22.00"), new BigDecimal("30000"),
                "KRW", new BigDecimal("34501"), null);

        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "5, 5, true",   // 정확히 일치
            "5, 8, true",   // +3일 경계값
            "5, 2, true",   // -3일 경계값
            "5, 9, false",  // +4일 초과
            "5, 1, false"   // -4일 초과
    })
    void 결제일이_기대일_기준_3일_이내면_통과한다(int expectedPayDay, int usedDayOfMonth, boolean expected) {
        boolean result = FixedExpenseMatchRules.isPayDayWithinWindow((short) expectedPayDay, usedDayOfMonth, 3);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void 기대결제일이_없으면_결제일_판정은_항상_통과한다() {
        boolean result = FixedExpenseMatchRules.isPayDayWithinWindow(null, 15, 3);

        assertThat(result).isTrue();
    }
}
