package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.CandidateSource;
import com.weaone.themoa.domain.policy.rag.dto.BenefitGroup;
import com.weaone.themoa.domain.policy.rag.dto.PolicyQuerySemantics;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchCondition;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchDiagnostics;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchIntent;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchPlan;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchResultItem;
import com.weaone.themoa.domain.policy.rag.dto.RecommendationTier;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 검색 실행 중 수집한 metrics를 기존 Diagnostics 응답 DTO로 조립한다.
 *
 * <p>PolicySearchResultAssembler 이후, PolicySearchResponse 반환 직전에 호출된다. 입력은 SearchPlan,
 * 후보 수집 metrics, 자격/랭킹 metrics, 지역 보장 선택 결과이며 출력은 기존 JSON 구조의
 * PolicySearchDiagnostics다.</p>
 *
 * <p>DB 또는 외부 시스템을 호출하지 않는다. API 호환성을 위해 record 구조는 유지하되 builder로
 * 필드명을 드러내며, 새 진단 필드를 추가할 때는 JSON 호환성과 builder 위치 검증 테스트를 함께 수정해야 한다.</p>
 */
@Component
public class PolicySearchDiagnosticsFactory {
    private final SupportIntentDetector supportIntentDetector = new SupportIntentDetector();
    private final BenefitGroupDetector benefitGroupDetector = new BenefitGroupDetector();

    /**
     * 후보 수집, hard filter, 지역 보장 선택 결과를 하나의 Diagnostics로 변환한다.
     *
     * <p>입력 객체들은 모두 검색 실행 중 실제로 사용된 값이다. Diagnostics는 화면 표시용이므로
     * 여기에서 검색 규칙을 다시 추론하거나 점수를 재계산하지 않는다.</p>
     */
    public PolicySearchDiagnostics create(PolicySearchPlan plan,
                                          PolicySearchIntent intent,
                                          PolicySearchConditionParser.ParsedPolicySearchCondition parsed,
                                          CandidateCollectionMetrics candidates,
                                          PolicySearchFilterMetrics filters,
                                          RegionCoverageResultSelector.Selection selection,
                                          List<PolicySearchResultItem> results,
                                          int semanticScoreCount,
                                          boolean retried,
                                          boolean mysqlFallbackUsed,
                                          String fallbackReason,
                                          long elapsedMillis,
                                          String userEmploymentStatus,
                                          boolean userEmploymentExplicit,
                                          String userEmploymentEvidence,
                                          String userEmploymentSource) {
        PolicySearchCondition condition = plan.condition();
        String positiveQuery = plan.explicitExclusion()
                ? String.join(" ", plan.normalizedGoal(), String.join(" ", plan.positiveTerms()))
                : plan.originalQuery();
        SupportIntentDetector.Detection supportIntentDetection = supportIntentDetector.detect(
                positiveQuery, condition.supportTypes(), plan.positiveTerms());
        BenefitGroupDetector.Detection benefitGroupDetection = benefitGroupDetector.detect(
                positiveQuery, condition.supportTypes(), plan.positiveTerms(), plan.desiredSupportIntents(), plan.desiredDomains());
        return PolicySearchDiagnosticsBuilder.builder()
                .vectorCandidateCount(candidates.vectorCandidateCount())
                .originalVectorCandidateCount(candidates.vectorOriginalCandidateCount())
                .intentVectorCandidateCount(candidates.vectorIntentCandidateCount())
                .expandedVectorCandidateCount(candidates.vectorExpandedCandidateCount())
                .categoryVectorCandidateCount(candidates.vectorCategoryCandidateCount())
                .lexicalCandidateCount(candidates.lexicalCandidateCount())
                .mysqlTitleCandidateCount(candidates.mysqlTitleCandidateCount())
                .mysqlKeywordCandidateCount(candidates.mysqlKeywordCandidateCount())
                .mysqlSummaryCandidateCount(candidates.mysqlSummaryCandidateCount())
                .mysqlCategoryCandidateCount(candidates.mysqlCategoryCandidateCount())
                .mergedCandidateCount(candidates.loadedPolicyCount())
                .duplicateCandidateCount(candidates.duplicateCandidateCount())
                .nationwideCandidateCount(filters.nationwideCandidateCount)
                .provinceMatchedCount(filters.provinceMatchedCount)
                .cityMatchedCount(filters.cityMatchedCount)
                .districtMatchedCount(filters.districtMatchedCount)
                .exactSigunguMatchedCount(filters.exactSigunguMatchedCount)
                .exactSidoMatchedCount(filters.exactSidoMatchedCount)
                .parentSidoMatchedCount(filters.parentSidoMatchedCount)
                .nationwideMatchedCount(filters.nationwideMatchedCount)
                .multipleRegionMatchedCount(filters.multipleRegionMatchedCount)
                .regionUnknownCount(filters.regionUnknownCount)
                .regionNotMatchedCount(filters.regionNotMatchedCount)
                .regionHardFilteredCount(filters.regionHardFilteredCount)
                .similarityPassedCount(semanticScoreCount)
                .databaseLoadedCount(candidates.loadedPolicyCount())
                .regionFilteredCount(filters.regionFiltered)
                .topicThresholdPassedCount(filters.topicThresholdPassedCount)
                .topicThresholdFailedCount(filters.topicThresholdFailedCount)
                .topicFilteredCount(filters.topicFilteredCount)
                .regionEligibleCount(filters.regionEligibleCount)
                .regionIneligibleCount(filters.regionIneligibleCount)
                .ageMatchedCount(filters.ageMatchedCount)
                .ageUnknownCount(filters.ageUnknownCount)
                .ageMismatchedCount(filters.ageMismatchedCount)
                .employmentMatchedCount(filters.employmentMatchedCount)
                .employmentUnknownCount(filters.employmentUnknownCount)
                .employmentMismatchedCount(filters.employmentMismatchedCount)
                .ageFilteredCount(filters.ageFiltered)
                .employmentFilteredCount(filters.employmentFiltered)
                .studentFilteredCount(filters.studentFiltered)
                .targetFilteredCount(filters.targetFiltered)
                .applicationFilteredCount(filters.applicationFiltered)
                .mysqlFallbackCount(candidates.fallbackAddedCount())
                .finalResultCount(results.size())
                .retriedWithLargerTopK(retried)
                .mysqlFallbackUsed(mysqlFallbackUsed)
                .searchMode(condition.searchMode().name())
                .regionExplicit(condition.regionExplicit())
                .ageExplicit(condition.ageExplicit())
                .employmentExplicit(condition.employmentExplicit())
                .studentExplicit(condition.studentExplicit())
                .regionFilterApplied(condition.regionExplicit())
                .ageFilterApplied(condition.ageExplicit())
                .employmentFilterApplied(condition.employmentExplicit())
                .studentFilterApplied(condition.studentExplicit())
                .coreKeywords(String.join(", ", condition.keywords()))
                .expandedKeywords(String.join(", ", condition.expandedKeywords()))
                .fallbackReason(fallbackReason)
                .elapsedTimeMs(elapsedMillis)
                .regionEligiblePoolCount(candidates.regionPoolTotal())
                .exactSigunguPoolCount(candidates.regionPoolExactSigungu())
                .parentSidoPoolCount(candidates.regionPoolParentSido())
                .nationwidePoolCount(candidates.regionPoolNationwide())
                .multipleRegionPoolCount(candidates.regionPoolMultiple())
                .unknownExcludedCount(filters.unknownExcludedCount)
                .wrongRegionExcludedCount(filters.wrongRegionExcludedCount)
                .exactSigunguSelectedCount(selection.exactSigunguSelectedCount())
                .parentSidoSelectedCount(selection.parentSidoSelectedCount())
                .nationwideSelectedCount(selection.nationwideSelectedCount())
                .unknownReviewResultCount(0)
                .normalizedGoal(plan.normalizedGoal())
                .desiredDomains(plan.desiredDomains().stream().map(Enum::name).sorted().collect(java.util.stream.Collectors.joining(", ")))
                .benefitGroups(plan.benefitGroups().stream().map(Enum::name).sorted().collect(java.util.stream.Collectors.joining(", ")))
                .benefitGroupEvidence(String.join(" | ", benefitGroupDetection.evidence()))
                .economicSubQueries(economicSubQueries(plan))
                .desiredSupportIntents(plan.desiredSupportIntents().stream().map(Enum::name).sorted().collect(java.util.stream.Collectors.joining(", ")))
                .supportIntentEvidence(String.join(" | ", supportIntentDetection.evidence()))
                .excludedDomains(plan.excludedDomains().stream().map(Enum::name).sorted().collect(java.util.stream.Collectors.joining(", ")))
                .positiveKeywords(String.join(", ", plan.positiveTerms()))
                .excludedKeywords(String.join(", ", plan.excludedTerms()))
                .explicitExclusion(plan.explicitExclusion())
                .semanticQuery(intent.semanticQuery())
                .originalVectorUsed(candidates.vectorOriginalCandidateCount() > 0)
                .normalizedVectorCandidateCount(candidates.vectorNormalizedCandidateCount())
                .excludedDomainFilteredCount(filters.excludedDomainFiltered)
                .userEducationStages(plan.userEducationStages().stream().map(Enum::name).sorted().collect(java.util.stream.Collectors.joining(", ")))
                .educationStageExplicit(plan.educationStageExplicit())
                .targetStageMismatchFilteredCount(filters.targetFiltered)
                .targetStageUnknownCount(filters.targetUnknownCount)
                .userEmploymentStatus(userEmploymentStatus)
                .userEmploymentExplicit(userEmploymentExplicit)
                .userEmploymentEvidence(userEmploymentEvidence)
                .userEmploymentAnalysisSource(userEmploymentSource)
                .employedMismatchFilteredCount(filters.employedMismatchFiltered)
                .unemployedMismatchFilteredCount(filters.unemployedMismatchFiltered)
                .primaryCandidateCount(filters.primaryCandidateCount)
                .needsConfirmationCandidateCount(filters.needsConfirmationCandidateCount)
                .needsConfirmationUsed(results.stream().anyMatch(item -> RecommendationTier.NEEDS_CONFIRMATION.name().equals(item.recommendationTier())))
                .broadFallbackUsed(candidates.fallbackAddedCount() > 0)
                .broadFallbackAddedCount(candidates.fallbackAddedCount())
                .semanticConflictDetected(semanticConflictDetected(parsed))
                .semanticConflictReason(semanticConflictReason(parsed))
                .initialMergedCandidateCount(candidates.initialMergedCandidateCount())
                .regionIntersectionCandidateCount(candidates.regionIntersectionCandidateCount())
                .eligibilityPassedCandidateCount(filters.eligibilityPassedCandidateCount)
                .topicGatePassedCandidateCount(filters.topicThresholdPassedCount)
                .topicGateFailedCandidateCount(filters.topicThresholdFailedCount)
                .firstRankingResultCount(filters.firstRankingResultCount)
                .rankingFallbackExecuted(filters.rankingFallbackExecuted)
                .fallbackReviewedCandidateCount(filters.fallbackReviewedCandidateCount)
                .fallbackPassedCandidateCount(filters.fallbackPassedCandidateCount)
                .finalCandidateCount(filters.finalCandidateCount)
                .topicFilteredSamples(filters.topicFilteredSamplesText())
                .workplaceRegion(region(condition.workplaceProvince(), condition.workplaceCity(), condition.workplaceDistrict()))
                .workplaceRawRegionText(condition.workplaceRawRegionText())
                .workplaceRegionResolutionStatus(condition.workplaceRegionResolutionStatus())
                .titleSearchMode(candidates.titleSearchMode())
                .normalizedQueryTitle(candidates.normalizedQueryTitle())
                .matchedPolicyTitle(candidates.matchedPolicyTitle())
                .titleMatchScore(candidates.titleMatchScore())
                .exactMatchCount(candidates.exactMatchCount())
                .broadSearchSuppressed(candidates.broadSearchSuppressed())
                .generalBenefitSubQueries(candidates.generalBenefitSubQueries())
                .generalBenefitVectorCounts(candidates.generalBenefitVectorCounts())
                .generalBenefitLexicalCounts(candidates.generalBenefitLexicalCounts())
                .build();
    }

    private boolean semanticConflictDetected(PolicySearchConditionParser.ParsedPolicySearchCondition parsed) {
        PolicyQuerySemantics semantics = parsed.semantics();
        if (semantics == null) {
            return false;
        }
        return semantics.desiredDomains().stream().anyMatch(semantics.excludedDomains()::contains);
    }

    private String economicSubQueries(PolicySearchPlan plan) {
        if (!plan.benefitGroups().contains(BenefitGroup.ECONOMIC_SUPPORT)) {
            return "";
        }
        String query = plan.originalQuery() == null ? "" : plan.originalQuery();
        return String.join(" | ",
                "CASH(" + priority(query, "지원금", "수당", "보조금", "장려금", "생활비", "활동비") + "): 청년 지원금 수당 보조금 장려금 생활비 활동비",
                "SAVINGS(" + priority(query, "통장", "저축", "계좌", "자산형성", "정부기여", "매칭") + "): 청년 저축 계좌 통장 자산형성 정부기여금 매칭 적립",
                "FINANCE(" + priority(query, "대출", "융자", "이자", "보증", "금융") + "): 청년 대출 융자 이자 지원 보증 금융 부담 완화",
                "LIVING_COST(" + priority(query, "월세", "주거비", "교통비", "K-패스", "케이패스", "환급", "할인", "바우처") + "): 청년 월세 주거비 교통비 환급 할인 바우처 비용 지원");
    }

    private String priority(String query, String... terms) {
        for (String term : terms) {
            if (query.contains(term)) {
                return "priority";
            }
        }
        return "normal";
    }

    private String semanticConflictReason(PolicySearchConditionParser.ParsedPolicySearchCondition parsed) {
        if (!semanticConflictDetected(parsed)) {
            return null;
        }
        return "OpenAI/Rule 의미 분석에서 원하는 분야와 제외 분야가 충돌했습니다.";
    }

    private String region(String province, String city, String district) {
        return java.util.stream.Stream.of(province, city, district)
                .filter(org.springframework.util.StringUtils::hasText)
                .collect(java.util.stream.Collectors.joining(" "));
    }
}
