package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.EducationStage;
import com.weaone.themoa.domain.policy.rag.dto.BenefitGroup;
import com.weaone.themoa.domain.policy.rag.dto.PolicyQuerySemantics;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchCondition;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchDiagnostics;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchIntent;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchMode;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchPlan;
import com.weaone.themoa.domain.policy.rag.dto.SearchDomain;
import com.weaone.themoa.domain.policy.rag.dto.SearchQueryType;
import com.weaone.themoa.domain.policy.rag.dto.SupportIntent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicySearchDiagnosticsFactoryTest {
    @Test
    void createsDiagnosticsFromNamedMetricsWithoutChangingJsonFields() {
        PolicySearchDiagnosticsFactory factory = new PolicySearchDiagnosticsFactory();
        PolicySearchFilterMetrics filters = new PolicySearchFilterMetrics();
        filters.regionFiltered = 2;
        filters.excludedDomainFiltered = 3;
        filters.primaryCandidateCount = 5;
        filters.needsConfirmationCandidateCount = 1;
        CandidateCollectionMetrics candidates = new CandidateCollectionMetrics(
                10, 4, 6, 0, 0, 0,
                7, 2, 3, 1, 1,
                8, 2, 0, 9, 4, 3, 2, 0);

        PolicySearchDiagnostics diagnostics = factory.create(
                plan(),
                intent(),
                new PolicySearchConditionParser.ParsedPolicySearchCondition(condition(), PolicyQuerySemantics.empty(), "TEST", false, null),
                candidates,
                filters,
                new RegionCoverageResultSelector.Selection(List.of(), 1, 2, 3),
                List.of(),
                6,
                true,
                false,
                "VECTOR_SEARCH_DISABLED",
                123,
                "EMPLOYED",
                true,
                "직장에 다님",
                "RULE_EXPLICIT");

        assertThat(diagnostics.vectorCandidateCount()).isEqualTo(10);
        assertThat(diagnostics.mysqlKeywordCandidateCount()).isEqualTo(3);
        assertThat(diagnostics.regionFilteredCount()).isEqualTo(2);
        assertThat(diagnostics.excludedDomainFilteredCount()).isEqualTo(3);
        assertThat(diagnostics.primaryCandidateCount()).isEqualTo(5);
        assertThat(diagnostics.needsConfirmationCandidateCount()).isEqualTo(1);
        assertThat(diagnostics.userEmploymentStatus()).isEqualTo("EMPLOYED");
        assertThat(diagnostics.fallbackReason()).isEqualTo("VECTOR_SEARCH_DISABLED");
        assertThat(diagnostics.desiredSupportIntents()).contains("CASH_ASSISTANCE");
        assertThat(diagnostics.benefitGroups()).contains("ECONOMIC_SUPPORT");
        assertThat(diagnostics.supportIntentEvidence()).contains("query");
    }

    private PolicySearchPlan plan() {
        return new PolicySearchPlan(SearchQueryType.BROAD_DISCOVERY, "청년 지원금", "청년 지원금",
                Set.of(SearchDomain.GENERAL), Set.of(), Set.of(SupportIntent.CASH_ASSISTANCE),
                Set.of(BenefitGroup.ECONOMIC_SUPPORT), Set.of(),
                Set.of("청년", "지원금"), Set.of(), condition(), Set.of(EducationStage.UNKNOWN), false, false, "TEST");
    }

    private PolicySearchCondition condition() {
        return new PolicySearchCondition(null, null, null, null, "EMPLOYED", null, null, "general",
                Set.of(), Set.of("청년"), Set.of("청년"), null, null, null, Set.of(),
                false, false, true, false, false, false, PolicySearchMode.HYBRID, 10);
    }

    private PolicySearchIntent intent() {
        return new PolicySearchIntent("청년 정책", Set.of(), Set.of("청년"), Set.of("청년"),
                "청년 정책", "청년");
    }
}
