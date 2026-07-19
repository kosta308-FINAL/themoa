package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.PolicySearchCondition;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchIntent;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchMode;
import com.weaone.themoa.domain.policy.rag.dto.PolicyQuerySemantics;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchPlan;
import com.weaone.themoa.domain.policy.rag.dto.SearchDomain;
import com.weaone.themoa.domain.policy.rag.dto.SearchQueryType;
import com.weaone.themoa.domain.policy.rag.dto.SupportIntent;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicySearchIntentBuilderTest {
    private final PolicySearchIntentBuilder builder = new PolicySearchIntentBuilder();

    @Test
    void separatesConditionTermsFromPolicyIntentAndBuildsEmploymentQueries() {
        PolicySearchCondition condition = new PolicySearchCondition("경기도", "수원시", null, 27,
                "UNEMPLOYED", null, null, "일자리", Set.of(), Set.of("청년"), Set.of("청년"),
                "수원", "EXACT", "SIGUNGU", Set.of(), true, true, true, false, true, false,
                PolicySearchMode.HYBRID, 20);

        PolicySearchIntent intent = builder.build("수원 사는 27살 취준생 정책", condition);

        assertThat(intent.conditionTerms()).contains("수원", "27살", "취준생");
        assertThat(intent.intentTerms()).contains("청년", "취업 지원", "구직 지원", "취업 준비");
        assertThat(intent.expandedIntentTerms()).contains("취업", "구직", "취업준비", "구직활동");
        assertThat(intent.semanticQuery()).contains("청년").contains("취업").doesNotContain("27살");
        assertThat(intent.lexicalQuery()).contains("구직").contains("면접");
    }

    @Test
    void expandsFinancialSupportIntentFromCategoryAndSupportTypes() {
        PolicySearchCondition condition = new PolicySearchCondition("경기도", "수원시", null, 27,
                null, null, "earlyCareer", "financialSupport", Set.of("CASH", "SUBSIDY"), Set.of("사회 초년생"),
                Set.of("사회 초년생"), "수원", "EXACT", "SIGUNGU", Set.of(), true, true, false, false,
                true, false, PolicySearchMode.HYBRID, 20);

        PolicySearchIntent intent = builder.build("수원 사는 27살 사회 초년생이 금융적으로 지원 받을 수 있는 정책", condition);

        assertThat(intent.conditionTerms()).contains("수원", "27살");
        assertThat(intent.intentTerms()).contains("청년", "금융 지원", "생활비 지원");
        assertThat(intent.expandedIntentTerms()).contains("금융", "생활비", "지원금", "사회초년생");
        assertThat(intent.semanticQuery()).isEqualTo("청년의 경제적 부담을 줄이는 지원 정책");
    }

    @Test
    void economicSupportIntentUsesBroadSemanticAndLexicalExpansion() {
        PolicySearchCondition condition = new PolicySearchCondition("경기도", "수원시", null, 27,
                "UNEMPLOYED", null, null, null, Set.of("CASH", "ALLOWANCE"), Set.of("지원금"),
                Set.of("지원금"), "수원", "EXACT", "SIGUNGU", Set.of(), true, true, true, false,
                false, true, PolicySearchMode.HYBRID, 20);

        PolicySearchIntent intent = builder.build("수원에 사는 27살 무직 청년인데 받을 수 있는 지원금이 있을까?", condition);

        assertThat(intent.intentTerms()).contains("경제·금전 지원");
        assertThat(intent.expandedIntentTerms()).contains("지원금", "저축", "대출", "주거비", "교통비", "바우처", "환급");
        assertThat(intent.semanticQuery()).isEqualTo("청년의 경제적 부담을 줄이는 지원 정책");
    }

    @Test
    void employmentExclusionDoesNotBuildEmploymentIntentTerms() {
        PolicySearchCondition condition = new PolicySearchCondition("경기도", "수원시", null, 22,
                null, true, null, null, Set.of(), Set.of("청년", "대학생", "취업"), Set.of("청년", "대학생", "취업", "구직", "일자리"),
                "수원", "EXACT", "SIGUNGU", Set.of(), true, true, false, true, false, false,
                PolicySearchMode.HYBRID, 20);
        PolicyQuerySemantics semantics = new PolicyQuerySemantics("대학생이 신청 가능한 청년 지원 정책",
                Set.of(), Set.of(SearchDomain.EMPLOYMENT), Set.of("대학생", "청년"), Set.of("취업", "구직", "일자리", "면접"), true);

        PolicySearchIntent intent = builder.build("수원 22살 대학생이고 취업 생각은 없어", condition, semantics);

        assertThat(intent.conditionTerms()).contains("수원", "22살");
        assertThat(intent.intentTerms()).doesNotContain("취업 지원", "구직 지원", "취업 준비");
        assertThat(intent.expandedIntentTerms()).doesNotContain("취업", "구직", "일자리", "면접");
        assertThat(intent.semanticQuery()).isEqualTo("대학생이 신청 가능한 청년 지원 정책");
        assertThat(intent.semanticQuery()).doesNotContain("취업", "구직", "일자리");
    }

    @Test
    void explicitExclusionPlanUsesPositiveTextOnly() {
        PolicySearchCondition condition = new PolicySearchCondition("경기도", null, null, null, null, null,
                null, null, Set.of(), Set.of("청년"), Set.of("청년"), "경기도", "EXACT", "SIDO",
                Set.of(), true, false, false, false, false, false, PolicySearchMode.HYBRID, 20);
        PolicySearchPlan plan = new PolicySearchPlan(SearchQueryType.TOPIC_SEARCH,
                "금융이나 대출은 빼고, 경기도 청년 문화·복지 혜택만 보여줘",
                "청년 문화 복지 혜택",
                Set.of(SearchDomain.CULTURE, SearchDomain.WELFARE),
                Set.of(SearchDomain.FINANCE),
                Set.of(),
                Set.of(SupportIntent.LOAN, SupportIntent.SAVINGS, SupportIntent.ASSET_BUILDING),
                Set.of("문화", "복지", "혜택"),
                Set.of("금융", "대출"),
                condition,
                Set.of(com.weaone.themoa.domain.policy.rag.dto.EducationStage.UNKNOWN),
                false,
                true,
                "TEST");

        PolicySearchIntent intent = builder.build(plan);

        assertThat(intent.originalQuery()).doesNotContain("대출");
        assertThat(intent.expandedIntentTerms()).doesNotContain("대출", "융자", "저축", "계좌", "통장");
        assertThat(intent.expandedIntentTerms()).contains("문화", "복지");
        assertThat(intent.semanticQuery()).isEqualTo("청년 문화 복지 혜택");
    }
}
