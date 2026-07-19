package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.SupportIntent;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SupportIntentDetectorTest {
    private final SupportIntentDetector detector = new SupportIntentDetector();

    @Test
    void detectsCashAssistanceExpressions() {
        assertThat(detector.detect("지원금 알려줘", Set.of(), Set.of()).intents()).contains(SupportIntent.CASH_ASSISTANCE);
        assertThat(detector.detect("받을 수 있는 지원금이 있을까?", Set.of(), Set.of()).intents()).contains(SupportIntent.CASH_ASSISTANCE);
        assertThat(detector.detect("금전적 혜택이 있는 정책", Set.of(), Set.of()).intents()).contains(SupportIntent.CASH_ASSISTANCE);
        assertThat(detector.detect("돈으로 지원받는 정책", Set.of(), Set.of()).intents()).contains(SupportIntent.CASH_ASSISTANCE);
        assertThat(detector.detect("K-패스 교통비 환급", Set.of(), Set.of()).intents()).contains(SupportIntent.CASH_ASSISTANCE);
    }

    @Test
    void mapsAllowanceLoanAndSavings() {
        assertThat(detector.detect("수당 받을 수 있나?", Set.of(), Set.of()).intents())
                .contains(SupportIntent.CASH_ASSISTANCE, SupportIntent.ALLOWANCE);
        assertThat(detector.detect("청년 대출", Set.of(), Set.of()).intents()).contains(SupportIntent.LOAN);
        assertThat(detector.detect("정부가 저축액을 매칭해 주는 정책", Set.of(), Set.of()).intents())
                .contains(SupportIntent.SAVINGS, SupportIntent.MATCHED_SAVINGS);
    }
}
