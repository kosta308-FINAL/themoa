package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCondition;
import com.weaone.themoa.domain.policy.policy.region.RegionCompatibility;
import com.weaone.themoa.domain.policy.policy.region.RegionMatchResult;
import com.weaone.themoa.domain.policy.rag.config.RagProperties;
import com.weaone.themoa.domain.policy.rag.dto.BenefitGroup;
import com.weaone.themoa.domain.policy.rag.dto.CandidateSource;
import com.weaone.themoa.domain.policy.rag.dto.ConditionMatchResult;
import com.weaone.themoa.domain.policy.rag.dto.EducationStage;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchCondition;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchMode;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchPlan;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchRequest;
import com.weaone.themoa.domain.policy.rag.dto.RecommendationTier;
import com.weaone.themoa.domain.policy.rag.dto.SearchDomain;
import com.weaone.themoa.domain.policy.rag.dto.SearchQueryType;
import com.weaone.themoa.domain.policy.rag.dto.SupportIntent;
import com.weaone.themoa.domain.policy.rag.dto.TargetStageMatchResult;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyRankingServiceTest {
    private final PolicyRankingService rankingService = new PolicyRankingService(
            new RagProperties(), new PolicyDomainClassifier(), new SearchDomainIntentPolicy());

    @Test
    void policyNameSearchKeepsExactTitleFirst() {
        Policy kpass = policy(1, "K-패스", PolicyCategory.복지);
        Policy other = policy(2, "청년 교통비 지원", PolicyCategory.복지);
        PolicyRankingResult result = rankingService.rank(context(plan(SearchQueryType.POLICY_NAME, "K-패스", Set.of(SearchDomain.GENERAL),
                        Set.of(SupportIntent.GENERAL))),
                new PolicyEvaluationResult(List.of(candidate(other, 0.9, 0.8, 0.0, RecommendationTier.PRIMARY),
                        candidate(kpass, 0.4, 0.2, 1.0, RecommendationTier.PRIMARY)), new PolicySearchFilterMetrics()));

        assertThat(result.rankedCandidates().get(0).candidate().policy().getTitle()).isEqualTo("K-패스");
        assertThat(result.rankedCandidates().get(0).ranking().finalRank()).isEqualTo(1);
    }

    @Test
    void generalSearchOrdersPrimaryBeforeNeedsConfirmationAndThenFinalScore() {
        Policy primaryLow = policy(1, "일반 청년 지원", PolicyCategory.복지);
        Policy needsHigh = policy(2, "확인 필요 지원", PolicyCategory.복지);
        Policy primaryHigh = policy(3, "높은 점수 지원", PolicyCategory.복지);
        PolicyRankingResult result = rankingService.rank(context(plan(SearchQueryType.BROAD_DISCOVERY, "청년 지원",
                        Set.of(SearchDomain.GENERAL), Set.of(SupportIntent.GENERAL))),
                new PolicyEvaluationResult(List.of(candidate(primaryLow, 0.5, 0.5, 0.0, RecommendationTier.PRIMARY),
                        candidate(needsHigh, 0.95, 0.95, 0.0, RecommendationTier.NEEDS_CONFIRMATION),
                        candidate(primaryHigh, 0.9, 0.9, 0.0, RecommendationTier.PRIMARY)), new PolicySearchFilterMetrics()));

        assertThat(result.rankedCandidates()).extracting(item -> item.candidate().policy().getId())
                .containsExactly(3, 1, 2);
    }

    @Test
    void exactSupportIntentDoesNotLowerScoreComparedWithNoSupportIntent() {
        Policy finance = policy(1, "청년 자산형성 지원금", PolicyCategory.금융);
        PolicyEvaluationResult candidates = new PolicyEvaluationResult(
                List.of(candidate(finance, 0.7, 0.7, 0.0, RecommendationTier.PRIMARY)), new PolicySearchFilterMetrics());

        double withIntent = rankingService.rank(context(plan(SearchQueryType.TOPIC_SEARCH, "금융 지원",
                        Set.of(SearchDomain.FINANCE), Set.of(SupportIntent.CASH_ASSISTANCE))), candidates)
                .rankedCandidates().get(0).ranking().finalScore();
        double withoutIntent = rankingService.rank(context(plan(SearchQueryType.TOPIC_SEARCH, "금융 지원",
                        Set.of(SearchDomain.FINANCE), Set.of())), candidates)
                .rankedCandidates().get(0).ranking().finalScore();

        assertThat(withIntent).isGreaterThanOrEqualTo(withoutIntent);
    }

    @Test
    void supportIntentBoostsMatchingPolicyWithoutHardFilteringUnknown() {
        Policy cash = policy(1, "청년 지원금", PolicyCategory.복지);
        Policy unknown = policy(2, "청년 상담", PolicyCategory.복지);

        PolicyRankingResult result = rankingService.rank(context(plan(SearchQueryType.BROAD_DISCOVERY, "지원금",
                        Set.of(), Set.of(SupportIntent.CASH_ASSISTANCE))),
                new PolicyEvaluationResult(List.of(candidate(unknown, 0.7, 0.7, 0.0, RecommendationTier.PRIMARY),
                        candidate(cash, 0.7, 0.7, 0.0, RecommendationTier.PRIMARY)), new PolicySearchFilterMetrics()));

        assertThat(result.rankedCandidates()).extracting(item -> item.candidate().policy().getId())
                .containsExactly(1, 2);
    }

    @Test
    void supportIntentDoesNotPromoteNeedsConfirmationTier() {
        Policy cash = policy(1, "청년 지원금", PolicyCategory.복지);

        PolicyRankingResult result = rankingService.rank(context(plan(SearchQueryType.BROAD_DISCOVERY, "지원금",
                        Set.of(), Set.of(SupportIntent.CASH_ASSISTANCE))),
                new PolicyEvaluationResult(List.of(candidate(cash, 0.7, 0.7, 0.0, RecommendationTier.NEEDS_CONFIRMATION)),
                        new PolicySearchFilterMetrics()));

        assertThat(result.rankedCandidates()).hasSize(1);
        assertThat(result.rankedCandidates().get(0).ranking().recommendationTier()).isEqualTo(RecommendationTier.NEEDS_CONFIRMATION);
    }

    @Test
    void economicBenefitGroupKeepsBroadEconomicPoliciesInRankingSignal() {
        Policy cash = policy(1, "청년 지원금", PolicyCategory.복지);
        Policy savings = policy(2, "청년 저축 계좌", PolicyCategory.금융);
        Policy loan = policy(3, "청년 대출 이자 지원", PolicyCategory.금융);
        Policy housing = policy(4, "청년 월세 지원", PolicyCategory.주거);

        PolicyRankingResult result = rankingService.rank(context(plan(SearchQueryType.BROAD_DISCOVERY, "지원금",
                        Set.of(), Set.of(SupportIntent.CASH_ASSISTANCE), Set.of(BenefitGroup.ECONOMIC_SUPPORT))),
                new PolicyEvaluationResult(List.of(candidate(cash, 0.7, 0.7, 0.0, RecommendationTier.PRIMARY),
                        candidate(savings, 0.7, 0.7, 0.0, RecommendationTier.PRIMARY),
                        candidate(loan, 0.7, 0.7, 0.0, RecommendationTier.PRIMARY),
                        candidate(housing, 0.7, 0.7, 0.0, RecommendationTier.PRIMARY)), new PolicySearchFilterMetrics()));

        assertThat(result.rankedCandidates()).hasSize(4);
        assertThat(result.rankedCandidates()).extracting(item -> item.candidate().policy().getId())
                .contains(1, 2, 3, 4);
        assertThat(result.rankedCandidates()).allSatisfy(item ->
                assertThat(item.ranking().supportIntentScore()).isGreaterThan(0));
    }

    @Test
    void broadEconomicPolicyRanksBeforeHighlyRestrictedVoucherForGeneralSearch() {
        Policy transit = policy(1, "청년 교통비 환급", PolicyCategory.복지, "19~39세 청년 누구나");
        Policy voucher = policy(2, "농식품 바우처", PolicyCategory.복지, "생계급여 수급 가구");

        PolicyRankingResult result = rankingService.rank(context(plan(SearchQueryType.BROAD_DISCOVERY, "청년 지원금",
                        Set.of(), Set.of(SupportIntent.CASH_ASSISTANCE), Set.of(BenefitGroup.ECONOMIC_SUPPORT))),
                new PolicyEvaluationResult(List.of(candidate(voucher, 0.75, 0.75, 0.0, RecommendationTier.PRIMARY),
                        candidate(transit, 0.72, 0.72, 0.0, RecommendationTier.PRIMARY)), new PolicySearchFilterMetrics()));

        assertThat(result.rankedCandidates()).extracting(item -> item.candidate().policy().getId())
                .containsExactly(1, 2);
    }

    @Test
    void restrictedPolicyPenaltyIsRelaxedWhenUserMentionsMatchingCondition() {
        Policy general = policy(1, "청년 통장", PolicyCategory.금융, "19~39세 청년 누구나");
        Policy artist = policy(2, "예술인 적립계좌", PolicyCategory.금융, "청년예술인 대상");

        PolicyRankingResult generalSearch = rankingService.rank(context(plan(SearchQueryType.BROAD_DISCOVERY, "청년 통장",
                        Set.of(), Set.of(SupportIntent.SAVINGS), Set.of(BenefitGroup.ECONOMIC_SUPPORT))),
                new PolicyEvaluationResult(List.of(candidate(artist, 0.8, 0.8, 0.0, RecommendationTier.PRIMARY),
                        candidate(general, 0.75, 0.75, 0.0, RecommendationTier.PRIMARY)), new PolicySearchFilterMetrics()));
        PolicyRankingResult artistSearch = rankingService.rank(context(plan(SearchQueryType.BROAD_DISCOVERY, "청년예술인 자산형성",
                        Set.of(), Set.of(SupportIntent.SAVINGS), Set.of(BenefitGroup.ECONOMIC_SUPPORT))),
                new PolicyEvaluationResult(List.of(candidate(artist, 0.8, 0.8, 0.0, RecommendationTier.PRIMARY),
                        candidate(general, 0.75, 0.75, 0.0, RecommendationTier.PRIMARY)), new PolicySearchFilterMetrics()));

        assertThat(generalSearch.rankedCandidates()).extracting(item -> item.candidate().policy().getId())
                .containsExactly(1, 2);
        assertThat(artistSearch.rankedCandidates().get(0).candidate().policy().getId()).isEqualTo(2);
    }

    @Test
    void ordersByRawFinalScoreBeforeRoundedDisplayScore() {
        Policy first = policy(1, "첫 번째 지원", PolicyCategory.복지);
        Policy second = policy(2, "두 번째 지원", PolicyCategory.복지);

        PolicyRankingResult result = rankingService.rank(context(plan(SearchQueryType.BROAD_DISCOVERY, "청년 지원",
                        Set.of(SearchDomain.GENERAL), Set.of(SupportIntent.GENERAL))),
                new PolicyEvaluationResult(List.of(candidate(second, 0.8236, 0.0, 0.0, RecommendationTier.PRIMARY),
                        candidate(first, 0.8244, 0.0, 0.0, RecommendationTier.PRIMARY)), new PolicySearchFilterMetrics()));

        assertThat(result.rankedCandidates()).extracting(item -> item.candidate().policy().getId())
                .containsExactly(1, 2);
        assertThat(result.rankedCandidates().get(0).ranking().rawFinalScore())
                .isGreaterThan(result.rankedCandidates().get(1).ranking().rawFinalScore());
    }

    @Test
    void broadDiscoveryRejectsCandidatesWithoutAnyTopicEvidence() {
        Policy unrelated = policy(1, "무관한 정책", PolicyCategory.복지);

        PolicyRankingResult result = rankingService.rank(context(plan(SearchQueryType.BROAD_DISCOVERY, "서울 27살 직장인 혜택",
                        Set.of(SearchDomain.GENERAL), Set.of())),
                new PolicyEvaluationResult(List.of(candidate(unrelated, 0.0, 0.0, 0.0, RecommendationTier.PRIMARY)),
                        new PolicySearchFilterMetrics()));

        assertThat(result.rankedCandidates()).isEmpty();
    }

    @Test
    void regionExplicitGeneralSearchOrdersRegionPriorityBeforeScore() {
        Policy daegu = policy(1, "대구광역시 청년 금융 지원", PolicyCategory.금융);
        Policy dalseo = policy(2, "대구 달서구 청년 금융 지원", PolicyCategory.금융);
        Policy multipleChild = policy(3, "대구 달서구 수성구 청년 금융 지원", PolicyCategory.금융);
        Policy multipleSido = policy(4, "대구 경북 청년 금융 지원", PolicyCategory.금융);
        Policy nationwide = policy(5, "전국 청년 금융 지원", PolicyCategory.금융);
        Policy unspecified = policy(6, "청년 금융 지원", PolicyCategory.금융);

        PolicyRankingResult result = rankingService.rank(context(regionalPlan(SearchQueryType.BROAD_DISCOVERY)),
                new PolicyEvaluationResult(List.of(
                        candidate(unspecified, 0.98, 0.98, 0.0, RecommendationTier.PRIMARY, RegionCompatibility.REGION_UNSPECIFIED),
                        candidate(nationwide, 1.0, 1.0, 0.0, RecommendationTier.PRIMARY, RegionCompatibility.NATIONWIDE),
                        candidate(multipleSido, 0.95, 0.95, 0.0, RecommendationTier.PRIMARY, RegionCompatibility.MULTIPLE_SIDO_MATCH),
                        candidate(multipleChild, 0.90, 0.90, 0.0, RecommendationTier.PRIMARY, RegionCompatibility.MULTIPLE_CHILD_SIGUNGU_MATCH),
                        candidate(dalseo, 0.80, 0.80, 0.0, RecommendationTier.PRIMARY, RegionCompatibility.CHILD_SIGUNGU_MATCH),
                        candidate(daegu, 0.55, 0.55, 0.0, RecommendationTier.PRIMARY, RegionCompatibility.EXACT_SIDO)),
                        new PolicySearchFilterMetrics()));

        assertThat(result.rankedCandidates()).extracting(item -> item.candidate().policy().getId())
                .containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    void sigunguRegionExplicitSearchOrdersChildParentAndNationwideInRegionOrder() {
        Policy dalseo = policy(1, "달서구 청년 지원", PolicyCategory.복지);
        Policy multipleSigungu = policy(2, "달서구 수성구 청년 지원", PolicyCategory.복지);
        Policy daegu = policy(3, "대구광역시 청년 지원", PolicyCategory.복지);
        Policy multipleSido = policy(4, "대구 경북 청년 지원", PolicyCategory.복지);
        Policy nationwide = policy(5, "전국 청년 지원", PolicyCategory.복지);

        PolicyRankingResult result = rankingService.rank(context(regionalSigunguPlan()),
                new PolicyEvaluationResult(List.of(
                        candidate(nationwide, 0.99, 0.99, 0.0, RecommendationTier.PRIMARY, RegionCompatibility.NATIONWIDE),
                        candidate(multipleSido, 0.92, 0.92, 0.0, RecommendationTier.PRIMARY, RegionCompatibility.MULTIPLE_SIDO_MATCH),
                        candidate(daegu, 0.88, 0.88, 0.0, RecommendationTier.PRIMARY, RegionCompatibility.PARENT_SIDO),
                        candidate(multipleSigungu, 0.70, 0.70, 0.0, RecommendationTier.PRIMARY, RegionCompatibility.MULTIPLE_SIGUNGU_MATCH),
                        candidate(dalseo, 0.60, 0.60, 0.0, RecommendationTier.PRIMARY, RegionCompatibility.EXACT_SIGUNGU)),
                        new PolicySearchFilterMetrics()));

        assertThat(result.rankedCandidates()).extracting(item -> item.candidate().policy().getId())
                .containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void policyNameSearchKeepsExactTitleBeforeRegionPriorityEvenWhenRegionExplicit() {
        Policy exactTitle = policy(1, "청년도약계좌", PolicyCategory.금융);
        Policy localHighScore = policy(2, "대구 청년 금융 지원", PolicyCategory.금융);

        PolicyRankingResult result = rankingService.rank(context(regionalPlan(SearchQueryType.POLICY_NAME)),
                new PolicyEvaluationResult(List.of(
                        candidate(localHighScore, 0.95, 0.95, 0.0, RecommendationTier.PRIMARY, RegionCompatibility.EXACT_SIDO),
                        candidate(exactTitle, 0.40, 0.40, 1.0, RecommendationTier.PRIMARY, RegionCompatibility.NATIONWIDE)),
                        new PolicySearchFilterMetrics()));

        assertThat(result.rankedCandidates()).extracting(item -> item.candidate().policy().getId())
                .containsExactly(1, 2);
    }

    @Test
    void regionUnsetSearchKeepsExistingScoreOrderWithoutRegionPriority() {
        Policy localLowScore = policy(1, "대구 청년 지원", PolicyCategory.복지);
        Policy nationwideHighScore = policy(2, "전국 청년 지원", PolicyCategory.복지);

        PolicyRankingResult result = rankingService.rank(context(plan(SearchQueryType.BROAD_DISCOVERY, "22살 청년 지원",
                        Set.of(SearchDomain.GENERAL), Set.of(SupportIntent.GENERAL))),
                new PolicyEvaluationResult(List.of(
                        candidate(localLowScore, 0.55, 0.55, 0.0, RecommendationTier.PRIMARY, RegionCompatibility.EXACT_SIDO),
                        candidate(nationwideHighScore, 0.90, 0.90, 0.0, RecommendationTier.PRIMARY, RegionCompatibility.NATIONWIDE)),
                        new PolicySearchFilterMetrics()));

        assertThat(result.rankedCandidates()).extracting(item -> item.candidate().policy().getId())
                .containsExactly(2, 1);
    }

    private PolicySearchExecutionContext context(PolicySearchPlan plan) {
        return new PolicySearchExecutionContext(new PolicySearchRequest(plan.originalQuery(), 10), plan, 1L);
    }

    private PolicySearchPlan plan(SearchQueryType type, String query, Set<SearchDomain> domains, Set<SupportIntent> intents) {
        return plan(type, query, domains, intents, Set.of());
    }

    private PolicySearchPlan plan(SearchQueryType type, String query, Set<SearchDomain> domains, Set<SupportIntent> intents,
                                  Set<BenefitGroup> benefitGroups) {
        return new PolicySearchPlan(type, query, query, domains, Set.of(), intents, benefitGroups, Set.of(),
                Set.of("청년"), Set.of(), condition(), Set.of(EducationStage.UNKNOWN), false, false, "TEST");
    }

    private PolicySearchPlan regionalPlan(SearchQueryType type) {
        PolicySearchCondition condition = new PolicySearchCondition("대구광역시", null, null, 22, null, null, null,
                "general", Set.of(), Set.of("청년", "금융"), Set.of("청년", "금융"), "대구", "RESOLVED", "SIDO",
                Set.of("대구광역시"), true, true, false, false, false, false, PolicySearchMode.HYBRID, 10);
        return new PolicySearchPlan(type, "대구에 사는 22살 청년 금융 지원", "청년 금융 지원",
                Set.of(SearchDomain.GENERAL), Set.of(), Set.of(SupportIntent.GENERAL), Set.of(),
                Set.of("청년", "금융"), Set.of(), condition, Set.of(EducationStage.UNKNOWN), false, false, "TEST");
    }

    private PolicySearchPlan regionalSigunguPlan() {
        PolicySearchCondition condition = new PolicySearchCondition("대구광역시", "달서구", null, 22, null, null, null,
                "general", Set.of(), Set.of("청년"), Set.of("청년"), "대구 달서구", "RESOLVED", "SIGUNGU",
                Set.of("대구광역시", "달서구"), true, true, false, false, false, false, PolicySearchMode.HYBRID, 10);
        return new PolicySearchPlan(SearchQueryType.BROAD_DISCOVERY, "대구 달서구 22살 청년 지원", "청년 지원",
                Set.of(SearchDomain.GENERAL), Set.of(), Set.of(SupportIntent.GENERAL), Set.of(),
                Set.of("청년"), Set.of(), condition, Set.of(EducationStage.UNKNOWN), false, false, "TEST");
    }

    private PolicySearchCondition condition() {
        return new PolicySearchCondition(null, null, null, null, null, null, null, "general",
                Set.of(), Set.of("청년"), Set.of("청년"), null, null, null, Set.of(),
                false, false, false, false, false, false, PolicySearchMode.HYBRID, 10);
    }

    private EvaluatedPolicyCandidate candidate(Policy policy, double semantic, double lexical, double title,
                                               RecommendationTier tier) {
        return candidate(policy, semantic, lexical, title, tier, RegionCompatibility.NATIONWIDE);
    }

    private EvaluatedPolicyCandidate candidate(Policy policy, double semantic, double lexical, double title,
                                               RecommendationTier tier, RegionCompatibility compatibility) {
        CandidateEvidence evidence = new CandidateEvidence(policy.getId(),
                title >= 1.0 ? List.of(new CandidateSourceEvidence(CandidateSource.EXACT_TITLE, 1, title, title, "TITLE")) : List.of(),
                semantic, lexical, title);
        PolicyEligibilityEvaluation eligibility = new PolicyEligibilityEvaluation(policy.getId(), true,
                new RegionMatchResult(compatibility, true, compatibility == RegionCompatibility.REGION_UNSPECIFIED ? 0 : 100,
                        compatibility.label()),
                ConditionMatchResult.unknown("나이 미입력"), ConditionMatchResult.unknown("취업 미입력"),
                ConditionMatchResult.unknown("학생 미입력"), TargetStageMatchResult.unknown("교육 미입력"),
                new EmploymentAudienceMatch(com.weaone.themoa.domain.policy.rag.dto.ConditionMatchStatus.UNKNOWN, "취업 미입력"),
                tier, List.of(), List.of(), null);
        return new EvaluatedPolicyCandidate(policy, evidence, eligibility);
    }

    private Policy policy(int id, String title, PolicyCategory category) {
        return policy(id, title, category, title);
    }

    private Policy policy(int id, String title, PolicyCategory category, String conditionSummary) {
        Policy policy = new Policy("P" + id);
        ReflectionTestUtils.setField(policy, "id", id);
        policy.updateBasic(title, "기관", category, title, null, null, null, true, true, "OPEN");
        policy.updateCondition(new PolicyCondition(18, 39, null, null, null, conditionSummary, false));
        return policy;
    }
}
