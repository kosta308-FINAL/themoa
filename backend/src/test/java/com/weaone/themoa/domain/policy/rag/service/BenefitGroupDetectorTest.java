package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.BenefitGroup;
import com.weaone.themoa.domain.policy.rag.dto.SearchDomain;
import com.weaone.themoa.domain.policy.rag.dto.SupportIntent;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BenefitGroupDetectorTest {
    private final BenefitGroupDetector detector = new BenefitGroupDetector();

    @Test
    void detectsEconomicSupportFromGrantExpressions() {
        assertThat(detect("지원금 알려줘")).contains(BenefitGroup.ECONOMIC_SUPPORT);
        assertThat(detect("금전적 혜택이 있는 정책")).contains(BenefitGroup.ECONOMIC_SUPPORT);
        assertThat(detect("생활비 부담을 줄일 정책")).contains(BenefitGroup.ECONOMIC_SUPPORT);
        assertThat(detect("받을 수 있는 돈이나 지원이 있을까")).contains(BenefitGroup.ECONOMIC_SUPPORT);
        assertThat(detect("K-패스 교통비 환급")).contains(BenefitGroup.ECONOMIC_SUPPORT);
    }

    @Test
    void detectsEconomicSupportForLoanAndHousingWithoutForcingFinanceDomain() {
        assertThat(detector.detect("청년 대출", Set.of(), Set.of(), Set.of(SupportIntent.LOAN),
                Set.of(SearchDomain.FINANCE)).groups()).contains(BenefitGroup.ECONOMIC_SUPPORT);
        assertThat(detector.detect("월세 지원금", Set.of(), Set.of(), Set.of(SupportIntent.HOUSING_COST),
                Set.of(SearchDomain.HOUSING)).groups()).contains(BenefitGroup.ECONOMIC_SUPPORT, BenefitGroup.HOUSING_SUPPORT);
    }

    private Set<BenefitGroup> detect(String query) {
        return detector.detect(query, Set.of(), Set.of(), Set.of(), Set.of()).groups();
    }
}
