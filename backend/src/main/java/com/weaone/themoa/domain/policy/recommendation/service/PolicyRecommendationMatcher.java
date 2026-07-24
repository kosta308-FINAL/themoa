package com.weaone.themoa.domain.policy.recommendation.service;

import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCondition;
import com.weaone.themoa.domain.policy.policy.region.RegionCompatibility;
import com.weaone.themoa.domain.policy.rag.dto.PolicyEmploymentAudience;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class PolicyRecommendationMatcher {

    public PolicyRecommendationMatch match(Policy policy, RegionCompatibility regionCompatibility, int age,
                                           UserEmploymentStatus employmentStatus,
                                           PolicyEmploymentAudience employmentAudience, LocalDate today) {
        if (policy == null || !policy.isActive()) {
            return PolicyRecommendationMatch.excluded();
        }
        if (!policy.isAlwaysOpen() && policy.getDueDate() != null && policy.getDueDate().isBefore(today)) {
            return PolicyRecommendationMatch.excluded();
        }
        PolicyCondition condition = policy.getCondition();
        if (!ageMatches(condition, age)) {
            return PolicyRecommendationMatch.excluded();
        }
        if (!employmentMatches(employmentStatus, employmentAudience)) {
            return PolicyRecommendationMatch.excluded();
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
                && employmentAudience.allowedStatuses().contains(employmentStatus)
                && !employmentAudience.allowedStatuses().contains(UserEmploymentStatus.UNKNOWN)) {
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
            return PolicyRecommendationMatch.excluded();
        }
        return new PolicyRecommendationMatch(true, score, trimReason(String.join(" · ", reasons)));
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

    private boolean employmentMatches(UserEmploymentStatus employmentStatus, PolicyEmploymentAudience employmentAudience) {
        if (employmentAudience == null || !employmentAudience.exclusive()) {
            return true;
        }
        return employmentAudience.allowedStatuses().contains(employmentStatus);
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
        return switch (compatibility) {
            case EXACT_SIGUNGU -> 40;
            case EXACT_SIDO, PARENT_SIDO -> 25;
            case NATIONWIDE -> 15;
            case MULTIPLE_REGION_MATCH -> 10;
            case UNKNOWN, NOT_MATCHED -> 0;
        };
    }

    private String regionReason(Policy policy, RegionCompatibility compatibility) {
        if (compatibility == RegionCompatibility.NATIONWIDE) {
            return "전국 대상";
        }
        if (compatibility == RegionCompatibility.EXACT_SIGUNGU) {
            return policy.getRegions().stream()
                    .map(policyRegion -> policyRegion.getRegion().getCity())
                    .filter(city -> city != null && !city.isBlank())
                    .findFirst()
                    .map(city -> city + " 거주 조건 일치")
                    .orElse("시·군·구 거주 조건 일치");
        }
        return "시·도 거주 조건 일치";
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
}
