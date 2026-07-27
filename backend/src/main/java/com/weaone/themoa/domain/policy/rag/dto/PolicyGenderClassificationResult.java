package com.weaone.themoa.domain.policy.rag.dto;

public record PolicyGenderClassificationResult(
        PolicyGenderAudience audience,
        boolean exclusive,
        double confidence,
        PolicyApplicantScope applicantScope,
        String evidence
) {
    public static PolicyGenderClassificationResult unknown(String evidence) {
        return new PolicyGenderClassificationResult(
                PolicyGenderAudience.UNKNOWN,
                false,
                0.0,
                PolicyApplicantScope.UNKNOWN,
                evidence == null || evidence.isBlank() ? "성별 조건을 확인할 수 없습니다." : evidence
        );
    }
}
