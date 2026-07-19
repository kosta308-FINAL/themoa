package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.region.RegionMatchResult;
import com.weaone.themoa.domain.policy.rag.dto.ConditionMatchResult;
import com.weaone.themoa.domain.policy.rag.dto.RecommendationTier;
import com.weaone.themoa.domain.policy.rag.dto.TargetStageMatchResult;

import java.util.List;

public record PolicyEligibilityEvaluation(
        int policyId,
        boolean passed,
        RegionMatchResult regionMatch,
        ConditionMatchResult ageMatch,
        ConditionMatchResult employmentMatch,
        ConditionMatchResult studentMatch,
        TargetStageMatchResult educationStageMatch,
        EmploymentAudienceMatch employmentAudienceMatch,
        RecommendationTier preliminaryTier,
        String preliminaryTierReason,
        List<String> matchedReasons,
        List<String> confirmationReasons,
        String excludedReason
) {
    public PolicyEligibilityEvaluation(int policyId,
                                       boolean passed,
                                       RegionMatchResult regionMatch,
                                       ConditionMatchResult ageMatch,
                                       ConditionMatchResult employmentMatch,
                                       ConditionMatchResult studentMatch,
                                       TargetStageMatchResult educationStageMatch,
                                       EmploymentAudienceMatch employmentAudienceMatch,
                                       RecommendationTier preliminaryTier,
                                       List<String> matchedReasons,
                                       List<String> confirmationReasons,
                                       String excludedReason) {
        this(policyId, passed, regionMatch, ageMatch, employmentMatch, studentMatch, educationStageMatch,
                employmentAudienceMatch, preliminaryTier, defaultReason(preliminaryTier), matchedReasons,
                confirmationReasons, excludedReason);
    }

    public PolicyEligibilityEvaluation {
        preliminaryTierReason = preliminaryTierReason == null ? defaultReason(preliminaryTier) : preliminaryTierReason;
        matchedReasons = matchedReasons == null ? List.of() : List.copyOf(matchedReasons);
        confirmationReasons = confirmationReasons == null ? List.of() : List.copyOf(confirmationReasons);
    }

    private static String defaultReason(RecommendationTier tier) {
        if (tier == RecommendationTier.NEEDS_CONFIRMATION) {
            return "명시되지 않은 자격 조건 확인이 필요합니다.";
        }
        if (tier == RecommendationTier.MISMATCH) {
            return "명시 조건과 정책 대상이 불일치합니다.";
        }
        return "사용자의 명시 조건과 충돌하지 않습니다.";
    }
}
