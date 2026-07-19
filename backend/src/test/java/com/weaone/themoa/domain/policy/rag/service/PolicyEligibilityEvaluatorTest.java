package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.domain.Policy;
import com.weaone.themoa.domain.policy.policy.domain.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.domain.PolicyCondition;
import com.weaone.themoa.domain.policy.policy.region.RegionCompatibility;
import com.weaone.themoa.domain.policy.policy.region.RegionMatchEvaluator;
import com.weaone.themoa.domain.policy.policy.region.RegionMatchResult;
import com.weaone.themoa.domain.policy.policy.region.ResolvedUserRegion;
import com.weaone.themoa.domain.policy.rag.config.RagProperties;
import com.weaone.themoa.domain.policy.rag.dto.ConditionMatchStatus;
import com.weaone.themoa.domain.policy.rag.dto.EducationStage;
import com.weaone.themoa.domain.policy.rag.dto.PolicyEmploymentAudience;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchCondition;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchMode;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchPlan;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchRequest;
import com.weaone.themoa.domain.policy.rag.dto.PolicyTargetAudienceClassification;
import com.weaone.themoa.domain.policy.rag.dto.SearchDomain;
import com.weaone.themoa.domain.policy.rag.dto.SearchQueryType;
import com.weaone.themoa.domain.policy.rag.dto.SupportIntent;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatus;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatusResult;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicyEligibilityEvaluatorTest {
    private final RegionMatchEvaluator regionMatchEvaluator = mock(RegionMatchEvaluator.class);
    private final ResolvedUserRegion userRegion = mock(ResolvedUserRegion.class);
    private final PolicyEligibilityEvaluator evaluator = new PolicyEligibilityEvaluator(
            new RagProperties(), regionMatchEvaluator, new PolicyTargetEligibilityFilter());

    @Test
    void removesExplicitOtherRegionAndKeepsParentSidoAndNationwide() {
        Policy seoul = policy(1, "서울 정책", null);
        Policy gyeonggi = policy(2, "경기 정책", null);
        Policy nationwide = policy(3, "전국 정책", null);
        when(regionMatchEvaluator.evaluate(eq(seoul), any())).thenReturn(region(RegionCompatibility.NOT_MATCHED, false));
        when(regionMatchEvaluator.evaluate(eq(gyeonggi), any())).thenReturn(region(RegionCompatibility.PARENT_SIDO, true));
        when(regionMatchEvaluator.evaluate(eq(nationwide), any())).thenReturn(region(RegionCompatibility.NATIONWIDE, true));

        PolicyEvaluationResult result = evaluator.evaluate(context(plan(condition("경기도", "수원시", 27, null, true, false),
                        Set.of(EducationStage.UNKNOWN), false)),
                collection(seoul, gyeonggi, nationwide), userRegion, Map.of(), Map.of(), UserEmploymentStatusResult.unknown());

        assertThat(result.passedCandidates()).extracting(item -> item.policy().getId()).containsExactly(2, 3);
        assertThat(result.metrics().regionFiltered).isEqualTo(1);
    }

    @Test
    void removesEmploymentAndEducationStageHardMismatchButKeepsUnknownAsConfirmation() {
        Policy unemployedOnly = policy(1, "미취업 전용", "UNEMPLOYED");
        Policy highSchoolOnly = policy(2, "고교생 전용", null);
        Policy universityOnly = policy(3, "대학생 전용", null);
        when(regionMatchEvaluator.evaluate(any(), any())).thenReturn(region(RegionCompatibility.NATIONWIDE, true));

        PolicySearchPlan universityPlan = plan(condition(null, null, null, "EMPLOYED", false, true),
                Set.of(EducationStage.UNIVERSITY), true);
        PolicyEvaluationResult mismatch = evaluator.evaluate(context(universityPlan),
                collection(unemployedOnly, highSchoolOnly), userRegion,
                Map.of(2, audience(EducationStage.HIGH_SCHOOL, true)),
                Map.of(1, new PolicyEmploymentAudience(Set.of(UserEmploymentStatus.UNEMPLOYED), true, 1.0, List.of("미취업자"))),
                new UserEmploymentStatusResult(UserEmploymentStatus.EMPLOYED, true, 1.0, List.of("직장")));

        assertThat(mismatch.passedCandidates()).isEmpty();
        assertThat(mismatch.metrics().employmentFiltered).isEqualTo(1);
        assertThat(mismatch.metrics().targetFiltered).isEqualTo(1);

        PolicyEvaluationResult unknownEducation = evaluator.evaluate(context(plan(condition(null, null, null, null, false, false),
                        Set.of(EducationStage.UNKNOWN), false)),
                collection(universityOnly), userRegion, Map.of(3, audience(EducationStage.UNIVERSITY, true)),
                Map.of(), UserEmploymentStatusResult.unknown());

        assertThat(unknownEducation.passedCandidates()).hasSize(1);
        assertThat(unknownEducation.passedCandidates().get(0).eligibility().preliminaryTier().name())
                .isEqualTo("NEEDS_CONFIRMATION");
        assertThat(unknownEducation.passedCandidates().get(0).eligibility().confirmationReasons())
                .anyMatch(reason -> reason.contains("대상 교육 단계 확인 필요"));
    }

    @Test
    void treatsZeroAgeBoundsAsUnknownInsteadOfHardMismatch() {
        Policy kpass = policy(4, "K-pass transport refund", null);
        kpass.updateCondition(new PolicyCondition(0, 0, null, null, null,
                "public transport refund for youth age 19 to 34", false));
        when(regionMatchEvaluator.evaluate(any(), any())).thenReturn(region(RegionCompatibility.NATIONWIDE, true));

        PolicyEvaluationResult result = evaluator.evaluate(
                context(plan(condition("Seoul", null, 27, "EMPLOYED", true, true),
                        Set.of(EducationStage.UNKNOWN), false)),
                collection(kpass), userRegion, Map.of(), Map.of(),
                new UserEmploymentStatusResult(UserEmploymentStatus.EMPLOYED, true, 1.0, List.of("employed")));

        assertThat(result.passedCandidates()).hasSize(1);
        assertThat(result.evaluationsByPolicyId().get(4).ageMatch().status()).isEqualTo(ConditionMatchStatus.UNKNOWN);
        assertThat(result.evaluationsByPolicyId().get(4).excludedReason()).isNull();
        assertThat(result.metrics().ageFiltered).isZero();
    }

    @Test
    void filtersOrganizationOnlyPolicyForIndividualButAllowsOrganizationUser() {
        Policy jointHiring = organizationPolicy(10, "벤처기업 공동채용 지원사업",
                "채용수요가 있는 벤처기업들이 합동으로 취업포털 사이트에 취업 공고를 게시하고 "
                        + "채용박람회를 통해 벤처기업의 우수 인재채용을 지원");
        when(regionMatchEvaluator.evaluate(any(), any())).thenReturn(region(RegionCompatibility.NATIONWIDE, true));

        PolicyEvaluationResult individual = evaluator.evaluate(
                context(plan("서울 회사에 다니고 있지만 다른 직장으로 옮기려고 해. 이직 지원 정책이 있을까?",
                        condition(null, null, null, "EMPLOYED", false, true))),
                collection(jointHiring), userRegion, Map.of(), Map.of(),
                new UserEmploymentStatusResult(UserEmploymentStatus.EMPLOYED, true, 1.0, List.of("직장인")));

        assertThat(individual.passedCandidates()).isEmpty();
        assertThat(individual.evaluationsByPolicyId().get(10).excludedReason()).isEqualTo("APPLICANT_AUDIENCE_MISMATCH");

        PolicyEvaluationResult organization = evaluator.evaluate(
                context(plan("우리 회사가 청년을 채용할 때 받을 수 있는 지원 알려줘",
                        condition(null, null, null, null, false, false))),
                collection(jointHiring), userRegion, Map.of(), Map.of(), UserEmploymentStatusResult.unknown());

        assertThat(organization.passedCandidates()).hasSize(1);
    }

    @Test
    void filtersOrganizationOnlyJointHiringForIndividualHighSchoolSearch() {
        Policy jointHiring = organizationPolicy(11, "벤처기업 공동채용 지원사",
                "채용수요가 있는 벤처기업들이 합동으로 취업포털 사이트에 취업 공고를 게시하고 "
                        + "채용박람회를 통해 벤처기업의 우수 인재채용을 지원");
        when(regionMatchEvaluator.evaluate(any(), any())).thenReturn(region(RegionCompatibility.NATIONWIDE, true));

        PolicyEvaluationResult result = evaluator.evaluate(
                context(plan("경기도에 사는 고3인데 취업이나 직업교육 관련 지원이 궁금해.",
                        condition("경기도", null, 18, null, true, false))),
                collection(jointHiring), userRegion, Map.of(), Map.of(), UserEmploymentStatusResult.unknown());

        assertThat(result.passedCandidates()).isEmpty();
        assertThat(result.evaluationsByPolicyId().get(11).excludedReason()).isEqualTo("APPLICANT_AUDIENCE_MISMATCH");
    }

    private PolicySearchExecutionContext context(PolicySearchPlan plan) {
        return new PolicySearchExecutionContext(new PolicySearchRequest("query", 10), plan, 1L);
    }

    private PolicyCandidateCollection collection(Policy... policies) {
        return new PolicyCandidateCollection(List.of(policies),
                java.util.Arrays.stream(policies).collect(java.util.stream.Collectors.toMap(Policy::getId,
                        policy -> new CandidateEvidence(policy.getId(), List.of(), 0.8, 0.5, 0.0))),
                Map.of(), Map.of(), Map.of(), Map.of(), new CandidateCollectionMetrics(0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, policies.length, 0, 0, 0, 0, 0, 0, 0), false, false, null);
    }

    private PolicySearchPlan plan(PolicySearchCondition condition, Set<EducationStage> stages, boolean educationExplicit) {
        return new PolicySearchPlan(SearchQueryType.ELIGIBILITY_SEARCH, "query", "query",
                Set.of(SearchDomain.GENERAL), Set.of(), Set.of(SupportIntent.GENERAL), Set.of(),
                Set.of(), Set.of(), condition, stages, educationExplicit, false, "TEST");
    }

    private PolicySearchPlan plan(String query, PolicySearchCondition condition) {
        return new PolicySearchPlan(SearchQueryType.ELIGIBILITY_SEARCH, query, query,
                Set.of(SearchDomain.EMPLOYMENT), Set.of(), Set.of(SupportIntent.EMPLOYMENT_SUPPORT), Set.of(),
                Set.of("이직", "채용"), Set.of(), condition, Set.of(EducationStage.UNKNOWN), false, false, "TEST");
    }

    private PolicySearchCondition condition(String province, String city, Integer age, String employment,
                                            boolean regionExplicit, boolean employmentExplicit) {
        return new PolicySearchCondition(province, city, null, age, employment, null, null, "general",
                Set.of(), Set.of("청년"), Set.of("청년"), city, null, null, Set.of(),
                regionExplicit, age != null, employmentExplicit, false, false, false, PolicySearchMode.HYBRID, 10);
    }

    private Policy policy(int id, String title, String employment) {
        Policy policy = new Policy("P" + id);
        ReflectionTestUtils.setField(policy, "id", id);
        policy.updateBasic(title, "기관", PolicyCategory.복지, title, null, null, null, true, true, "OPEN");
        policy.updateCondition(new PolicyCondition(18, 39, employment, null, null, title, false));
        return policy;
    }

    private Policy organizationPolicy(int id, String title, String summary) {
        Policy policy = new Policy("P" + id);
        ReflectionTestUtils.setField(policy, "id", id);
        policy.updateBasic(title, "기관", PolicyCategory.일자리, summary, null, null, null, true, true, "OPEN");
        policy.updateCondition(new PolicyCondition(18, 39, null, null, null, summary, false));
        return policy;
    }

    private RegionMatchResult region(RegionCompatibility compatibility, boolean eligible) {
        return new RegionMatchResult(compatibility, eligible, eligible ? 100 : 0, compatibility.name());
    }

    private PolicyTargetAudienceClassification audience(EducationStage stage, boolean exclusive) {
        return new PolicyTargetAudienceClassification(Set.of(stage), Set.of(), exclusive, 1.0, List.of(stage.name()));
    }
}
