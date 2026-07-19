package com.weaone.themoa.domain.policy.rag.service;

/**
 * 검색 후보 평가와 hard filter 과정에서 발생한 카운터를 모은다.
 *
 * <p>지역/나이/취업/교육 단계처럼 명확한 자격 조건은 랭킹 점수가 아니라
 * 필터 단계에서 제거되므로, Diagnostics도 이 단계의 카운터를 기준으로 조립해야 한다.</p>
 */
public class PolicySearchFilterMetrics {
    int initialMergedCandidateCount;
    int regionIntersectionCandidateCount;
    int eligibilityPassedCandidateCount;
    int regionFiltered;
    int unknownExcludedCount;
    int wrongRegionExcludedCount;
    int nationwideCandidateCount;
    int provinceMatchedCount;
    int cityMatchedCount;
    int districtMatchedCount;
    int exactSigunguMatchedCount;
    int exactSidoMatchedCount;
    int parentSidoMatchedCount;
    int nationwideMatchedCount;
    int multipleRegionMatchedCount;
    int regionUnknownCount;
    int regionNotMatchedCount;
    int regionHardFilteredCount;
    int topicThresholdPassedCount;
    int topicThresholdFailedCount;
    int topicFilteredCount;
    int regionEligibleCount;
    int regionIneligibleCount;
    int ageMatchedCount;
    int ageUnknownCount;
    int ageMismatchedCount;
    int employmentMatchedCount;
    int employmentUnknownCount;
    int employmentMismatchedCount;
    int ageFiltered;
    int employmentFiltered;
    int studentFiltered;
    int targetFiltered;
    int targetUnknownCount;
    int employedMismatchFiltered;
    int unemployedMismatchFiltered;
    int primaryCandidateCount;
    int needsConfirmationCandidateCount;
    int applicationFiltered;
    int excludedDomainFiltered;
    int firstRankingResultCount;
    boolean rankingFallbackExecuted;
    int fallbackReviewedCandidateCount;
    int fallbackPassedCandidateCount;
    int finalCandidateCount;
    private final java.util.List<String> topicFilteredSamples = new java.util.ArrayList<>();

    boolean hasNoTopicRelevantCandidate() {
        return (topicFilteredCount > 0 || topicThresholdFailedCount > 0)
                && topicThresholdPassedCount == 0;
    }

    void addTopicFilteredSample(Integer policyId,
                                String title,
                                CandidateEvidence evidence,
                                com.weaone.themoa.domain.policy.rag.dto.TopicRelevanceScore topicScore,
                                double benefitGroupScore,
                                String eligibilityBreadth,
                                String reason) {
        if (topicFilteredSamples.size() >= 20) {
            return;
        }
        topicFilteredSamples.add("policyId=" + policyId
                + ", title=" + title
                + ", rawSemanticScore=" + round(evidence.rawSemanticScore())
                + ", normalizedSemanticScore=" + round(evidence.normalizedSemanticScore())
                + ", weightedSemanticScore=" + round(topicScore.semanticScore())
                + ", lexicalScore=" + round(evidence.lexicalScore())
                + ", benefitGroupScore=" + round(benefitGroupScore)
                + ", eligibilityBreadth=" + eligibilityBreadth
                + ", topicScore=" + round(topicScore.finalTopicScore())
                + ", reason=" + reason);
    }

    String topicFilteredSamplesText() {
        return String.join(" | ", topicFilteredSamples);
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
