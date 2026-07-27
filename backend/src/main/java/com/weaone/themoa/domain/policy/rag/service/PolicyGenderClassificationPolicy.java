package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.PolicyApplicantScope;
import com.weaone.themoa.domain.policy.rag.dto.PolicyGenderAudience;
import com.weaone.themoa.domain.policy.rag.dto.PolicyGenderClassificationResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PolicyGenderClassificationPolicy {
    public static final String VERSION = "gender-v1";
    public static final double HARD_FILTER_CONFIDENCE_THRESHOLD = 0.70;

    public PolicyGenderClassificationResult normalize(PolicyGenderClassificationResult result) {
        if (result == null || result.audience() == null || result.applicantScope() == null) {
            return PolicyGenderClassificationResult.unknown("성별 분류 응답이 비어 있습니다.");
        }
        if (Double.isNaN(result.confidence()) || result.confidence() < 0.0 || result.confidence() > 1.0) {
            return PolicyGenderClassificationResult.unknown("성별 분류 신뢰도 값이 유효하지 않습니다.");
        }
        if (!StringUtils.hasText(result.evidence())) {
            return PolicyGenderClassificationResult.unknown("성별 분류 근거가 비어 있습니다.");
        }
        if (specific(result.audience()) && !result.exclusive()) {
            return PolicyGenderClassificationResult.unknown("특정 성별 정책이지만 배타 조건으로 확인되지 않았습니다.");
        }
        if (specific(result.audience()) && result.confidence() < HARD_FILTER_CONFIDENCE_THRESHOLD) {
            return PolicyGenderClassificationResult.unknown("특정 성별 제한 신뢰도가 기준보다 낮습니다.");
        }
        if (specific(result.audience()) && result.applicantScope() != PolicyApplicantScope.INDIVIDUAL) {
            return PolicyGenderClassificationResult.unknown("개인 신청자의 성별 제한으로 확인되지 않았습니다.");
        }
        if (result.audience() == PolicyGenderAudience.ALL && result.exclusive()) {
            return new PolicyGenderClassificationResult(PolicyGenderAudience.ALL, false,
                    result.confidence(), result.applicantScope(), result.evidence());
        }
        if (result.audience() == PolicyGenderAudience.UNKNOWN && result.exclusive()) {
            return PolicyGenderClassificationResult.unknown(result.evidence());
        }
        return result;
    }

    public boolean hardFilterable(PolicyGenderClassificationResult result) {
        PolicyGenderClassificationResult normalized = normalize(result);
        return specific(normalized.audience())
                && normalized.exclusive()
                && normalized.confidence() >= HARD_FILTER_CONFIDENCE_THRESHOLD
                && normalized.applicantScope() == PolicyApplicantScope.INDIVIDUAL
                && StringUtils.hasText(normalized.evidence());
    }

    private boolean specific(PolicyGenderAudience audience) {
        return audience == PolicyGenderAudience.MALE_ONLY || audience == PolicyGenderAudience.FEMALE_ONLY;
    }
}
