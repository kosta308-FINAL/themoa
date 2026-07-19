package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.PolicyQuerySemantics;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchCondition;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchMode;
import com.weaone.themoa.domain.policy.rag.dto.SearchDomain;
import com.weaone.themoa.domain.policy.rag.dto.SupportIntent;
import com.weaone.themoa.domain.policy.rag.dto.BenefitGroup;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicySearchPlanServiceTest {
    private final CompositePolicySearchConditionParser parser = mock(CompositePolicySearchConditionParser.class);
    private final PolicySearchPlanService service = new PolicySearchPlanService(
            parser,
            new PolicyQueryClassifier(new PolicyKeywordNormalizer()),
            new SearchDomainIntentPolicy(),
            new UserEducationStageDetector(),
            new SupportIntentDetector(),
            new BenefitGroupDetector());

    @Test
    void connectsConditionSupportTypesToPlan() {
        stub("지원금", condition(null, Set.of("CASH", "ALLOWANCE", "SUBSIDY")),
                semantics(Set.of(), Set.of("지원금")));

        var plan = service.build("지원금", 20).plan();

        assertThat(plan.desiredSupportIntents()).contains(SupportIntent.CASH_ASSISTANCE, SupportIntent.ALLOWANCE);
        assertThat(plan.benefitGroups()).contains(BenefitGroup.ECONOMIC_SUPPORT);
    }

    @Test
    void genericGrantDoesNotForceFinanceDomain() {
        stub("지원금 알려줘", condition(null, Set.of("CASH")), semantics(Set.of(SearchDomain.FINANCE), Set.of("지원금")));

        var plan = service.build("지원금 알려줘", 20).plan();

        assertThat(plan.desiredDomains()).doesNotContain(SearchDomain.FINANCE);
        assertThat(plan.desiredSupportIntents()).contains(SupportIntent.CASH_ASSISTANCE);
        assertThat(plan.benefitGroups()).contains(BenefitGroup.ECONOMIC_SUPPORT);
    }

    @Test
    void keepsDomainWhenQueryContainsSpecificDomainEvidence() {
        stub("월세 지원금", condition("주거", Set.of("CASH")), semantics(Set.of(SearchDomain.HOUSING), Set.of("월세", "지원금")));
        assertThat(service.build("월세 지원금", 20).plan().desiredDomains()).contains(SearchDomain.HOUSING);
        assertThat(service.build("월세 지원금", 20).plan().desiredSupportIntents())
                .contains(SupportIntent.CASH_ASSISTANCE, SupportIntent.HOUSING_COST);
        assertThat(service.build("월세 지원금", 20).plan().benefitGroups())
                .contains(BenefitGroup.ECONOMIC_SUPPORT, BenefitGroup.HOUSING_SUPPORT);

        stub("면접 지원금", condition("일자리", Set.of("CASH")), semantics(Set.of(SearchDomain.EMPLOYMENT), Set.of("면접", "지원금")));
        assertThat(service.build("면접 지원금", 20).plan().desiredDomains()).contains(SearchDomain.EMPLOYMENT);
        assertThat(service.build("면접 지원금", 20).plan().desiredSupportIntents()).contains(SupportIntent.CASH_ASSISTANCE);

        stub("청년 대출", condition("금융", Set.of()), semantics(Set.of(SearchDomain.FINANCE), Set.of("대출")));
        assertThat(service.build("청년 대출", 20).plan().desiredDomains()).contains(SearchDomain.FINANCE);
        assertThat(service.build("청년 대출", 20).plan().desiredSupportIntents()).contains(SupportIntent.LOAN);
        assertThat(service.build("청년 대출", 20).plan().benefitGroups()).contains(BenefitGroup.ECONOMIC_SUPPORT);
    }

    @Test
    void excludedFinanceAndLoanDoNotReenterPositiveIntent() {
        String query = "금융이나 대출은 빼고, 경기도 청년 문화·복지 혜택만 보여줘";
        stub(query, condition(null, Set.of()), new PolicyQuerySemantics("청년 문화 복지 혜택",
                Set.of(SearchDomain.CULTURE, SearchDomain.WELFARE),
                Set.of(SearchDomain.FINANCE),
                Set.of("문화", "복지", "혜택"),
                Set.of("금융", "대출"),
                true));

        var plan = service.build(query, 20).plan();

        assertThat(plan.desiredDomains()).contains(SearchDomain.CULTURE, SearchDomain.WELFARE);
        assertThat(plan.excludedDomains()).contains(SearchDomain.FINANCE);
        assertThat(plan.desiredSupportIntents()).doesNotContain(SupportIntent.LOAN, SupportIntent.SAVINGS, SupportIntent.ASSET_BUILDING);
        assertThat(plan.excludedSupportIntents()).contains(SupportIntent.LOAN, SupportIntent.SAVINGS, SupportIntent.ASSET_BUILDING);
        assertThat(plan.benefitGroups()).doesNotContain(BenefitGroup.ECONOMIC_SUPPORT);
    }

    private void stub(String query, PolicySearchCondition condition, PolicyQuerySemantics semantics) {
        when(parser.parse(query, 20)).thenReturn(new PolicySearchConditionParser.ParsedPolicySearchCondition(
                condition, semantics, "TEST", false, null));
    }

    private PolicySearchCondition condition(String category, Set<String> supportTypes) {
        return new PolicySearchCondition(null, null, null, null, null, null, null, category,
                supportTypes, Set.of("청년"), Set.of("청년"), null, null, null, Set.of(),
                false, false, false, false, category != null, !supportTypes.isEmpty(), PolicySearchMode.HYBRID, 20);
    }

    private PolicyQuerySemantics semantics(Set<SearchDomain> domains, Set<String> keywords) {
        return new PolicyQuerySemantics("청년 지원 정책", domains, Set.of(), keywords, Set.of(), false);
    }
}
