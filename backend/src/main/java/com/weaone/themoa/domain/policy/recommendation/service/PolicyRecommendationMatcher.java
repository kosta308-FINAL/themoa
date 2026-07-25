package com.weaone.themoa.domain.policy.recommendation.service;

import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCondition;
import com.weaone.themoa.domain.policy.policy.region.RegionCompatibility;
import com.weaone.themoa.domain.policy.rag.dto.ConditionMatchStatus;
import com.weaone.themoa.domain.policy.rag.dto.PolicyEmploymentAudience;
import com.weaone.themoa.domain.policy.rag.dto.PolicyTargetAudienceClassification;
import com.weaone.themoa.domain.policy.rag.dto.TargetStageMatchResult;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatus;
import com.weaone.themoa.domain.policy.rag.service.PolicyTargetEligibilityFilter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class PolicyRecommendationMatcher {
    private final PolicyTargetEligibilityFilter targetEligibilityFilter;

    public PolicyRecommendationMatcher(PolicyTargetEligibilityFilter targetEligibilityFilter) {
        this.targetEligibilityFilter = targetEligibilityFilter;
    }

    public PolicyRecommendationMatch match(Policy policy, RegionCompatibility regionCompatibility, int age,
                                           UserEmploymentStatus employmentStatus,
                                           PolicyEmploymentAudience employmentAudience,
                                           PolicyTargetAudienceClassification targetAudience,
                                           LocalDate today) {
        return match(policy, regionCompatibility, age, employmentStatus, employmentAudience, targetAudience, today, false);
    }

    public PolicyRecommendationMatch match(Policy policy, RegionCompatibility regionCompatibility, int age,
                                           UserEmploymentStatus employmentStatus,
                                           PolicyEmploymentAudience employmentAudience,
                                           PolicyTargetAudienceClassification targetAudience,
                                           LocalDate today,
                                           boolean regionClassificationConflict) {
        if (policy == null || !policy.isActive()) {
            return PolicyRecommendationMatch.excluded("INACTIVE");
        }
        if (!policy.isAlwaysOpen() && policy.getDueDate() != null && policy.getDueDate().isBefore(today)) {
            return PolicyRecommendationMatch.excluded("EXPIRED");
        }
        PolicyCondition condition = policy.getCondition();
        if (!ageMatches(condition, age)) {
            return PolicyRecommendationMatch.excluded("AGE_MISMATCH");
        }
        if (regionCompatibility == RegionCompatibility.NOT_MATCHED
                || regionCompatibility == RegionCompatibility.UNKNOWN
                || regionCompatibility == null) {
            return PolicyRecommendationMatch.excluded("REGION_MISMATCH");
        }
        if (regionClassificationConflict) {
            return PolicyRecommendationMatch.excluded("REGION_CLASSIFICATION_CONFLICT");
        }
        EmploymentMatchResult employmentMatchResult = employmentMatches(employmentStatus, employmentAudience);
        if (!employmentMatchResult.matched()) {
            return PolicyRecommendationMatch.excluded(employmentMatchResult.reason());
        }
        TargetMatchResult targetMatchResult = targetAudienceMatches(targetAudience);
        if (!targetMatchResult.matched()) {
            return PolicyRecommendationMatch.excluded(targetMatchResult.reason());
        }

        int score = 0;
        List<String> reasons = new ArrayList<>();
        int regionScore = regionScore(regionCompatibility);
        if (regionScore > 0) {
            score += regionScore;
            reasons.add(regionReason(policy, regionCompatibility));
        }
        if (hasAgeCondition(condition)) {
            score += 25;
            reasons.add("만 " + age + "세 연령 조건 일치");
        }
        if (employmentAudience != null
                && employmentAudience.exclusive()
                && !employmentAudience.conflict()
                && employmentAudience.allowedStatuses().size() == 1
                && employmentAudience.allowedStatuses().contains(employmentStatus)) {
            score += 20;
            reasons.add(employmentLabel(employmentStatus) + " 대상 조건 일치");
        }
        if (policy.isAlwaysOpen() || isOpenNow(policy, today)) {
            score += 10;
            reasons.add("현재 신청 가능");
        } else if (policy.getStartDate() != null && policy.getStartDate().isAfter(today)) {
            score += 5;
            reasons.add("신청 시작 예정");
        }
        if (score <= 0) {
            return PolicyRecommendationMatch.excluded("NO_SCORE");
        }
        return new PolicyRecommendationMatch(true, score, regionCompatibility, trimReason(String.join(" · ", reasons)));
    }

    private boolean ageMatches(PolicyCondition condition, int age) {
        if (condition == null) {
            return true;
        }
        if (condition.getMinAge() != null && age < condition.getMinAge()) {
            return false;
        }
        return condition.getMaxAge() == null || age <= condition.getMaxAge();
    }

    private boolean hasAgeCondition(PolicyCondition condition) {
        return condition != null && (condition.getMinAge() != null || condition.getMaxAge() != null);
    }

    private EmploymentMatchResult employmentMatches(UserEmploymentStatus employmentStatus, PolicyEmploymentAudience employmentAudience) {
        if (employmentAudience == null) {
            return EmploymentMatchResult.excluded("MISSING_SEARCH_PROJECTION");
        }
        if (employmentAudience.conflict()) {
            return EmploymentMatchResult.excluded("EMPLOYMENT_CONFLICT");
        }
        if (employmentStatus == UserEmploymentStatus.EMPLOYED && employmentAudience.employmentTransitionProgram()) {
            return EmploymentMatchResult.excluded("EMPLOYMENT_MISMATCH");
        }
        if (!employmentAudience.exclusive()) {
            return EmploymentMatchResult.included();
        }
        if (employmentAudience.allowedStatuses().contains(employmentStatus)) {
            return EmploymentMatchResult.included();
        }
        return EmploymentMatchResult.excluded("EMPLOYMENT_MISMATCH");
    }

    private TargetMatchResult targetAudienceMatches(PolicyTargetAudienceClassification targetAudience) {
        if (targetAudience == null) {
            return TargetMatchResult.excluded("MISSING_SEARCH_PROJECTION");
        }
        TargetStageMatchResult result = targetEligibilityFilter.matchAutomaticRecommendation(targetAudience);
        if (result.status() == ConditionMatchStatus.MISMATCH) {
            return TargetMatchResult.excluded("EDUCATION_STAGE_MISMATCH");
        }
        return TargetMatchResult.included();
    }

    private boolean isOpenNow(Policy policy, LocalDate today) {
        boolean afterStart = policy.getStartDate() == null || !policy.getStartDate().isAfter(today);
        boolean beforeDue = policy.getDueDate() == null || !policy.getDueDate().isBefore(today);
        return afterStart && beforeDue;
    }

    private int regionScore(RegionCompatibility compatibility) {
        if (compatibility == null) {
            return 0;
        }
        return compatibility.recommendationScore();
    }

    private String regionReason(Policy policy, RegionCompatibility compatibility) {
        if (compatibility == RegionCompatibility.NATIONWIDE) {
            return "전국 대상 정책";
        }
        if (compatibility == RegionCompatibility.REGION_UNSPECIFIED) {
            return "지역 제한이 명시되지 않은 정책";
        }
        if (compatibility == RegionCompatibility.EXACT_SIGUNGU) {
            return sigunguName(policy) + " 거주 조건 일치";
        }
        if (compatibility == RegionCompatibility.MULTIPLE_SIGUNGU_MATCH) {
            return "복수 대상 지역 중 " + sigunguName(policy) + " 포함";
        }
        if (compatibility == RegionCompatibility.MULTIPLE_SIDO_MATCH) {
            return "복수 대상 시·도 중 " + sidoName(policy) + " 포함";
        }
        if (compatibility == RegionCompatibility.EXACT_SIDO || compatibility == RegionCompatibility.PARENT_SIDO) {
            return sidoName(policy) + " 거주 조건 일치";
        }
        return "지역 조건 일치";
    }

    private String sigunguName(Policy policy) {
        return policy.getRegions().stream()
                .map(policyRegion -> policyRegion.getRegion().getCity())
                .filter(city -> city != null && !city.isBlank())
                .findFirst()
                .orElse("시·군·구");
    }

    private String sidoName(Policy policy) {
        return policy.getRegions().stream()
                .map(policyRegion -> policyRegion.getRegion().getProvince())
                .filter(province -> province != null && !province.isBlank())
                .findFirst()
                .orElse("시·도");
    }

    private String employmentLabel(UserEmploymentStatus employmentStatus) {
        return switch (employmentStatus) {
            case EMPLOYED -> "재직자";
            case UNEMPLOYED -> "미취업";
            case UNKNOWN -> "취업 상태";
        };
    }

    private String trimReason(String reason) {
        if (reason.length() <= 500) {
            return reason;
        }
        return reason.substring(0, 500);
    }

    private record EmploymentMatchResult(boolean matched, String reason) {
        static EmploymentMatchResult included() {
            return new EmploymentMatchResult(true, "");
        }

        static EmploymentMatchResult excluded(String reason) {
            return new EmploymentMatchResult(false, reason);
        }
    }

    private record TargetMatchResult(boolean matched, String reason) {
        static TargetMatchResult included() {
            return new TargetMatchResult(true, "");
        }

        static TargetMatchResult excluded(String reason) {
            return new TargetMatchResult(false, reason);
        }
    }
}
