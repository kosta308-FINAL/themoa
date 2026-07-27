package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.PolicyApplicantScope;
import com.weaone.themoa.domain.policy.rag.dto.PolicyGenderAudience;
import com.weaone.themoa.domain.policy.rag.dto.PolicyGenderClassificationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyGenderClassificationPolicyTest {
    private final PolicyGenderClassificationPolicy policy = new PolicyGenderClassificationPolicy();

    @Test
    void specificGenderWithExclusiveFalseIsNotHardFilterable() {
        PolicyGenderClassificationResult result = policy.normalize(new PolicyGenderClassificationResult(
                PolicyGenderAudience.FEMALE_ONLY, false, 0.95, PolicyApplicantScope.INDIVIDUAL, "여성 대상"));

        assertThat(result.audience()).isEqualTo(PolicyGenderAudience.UNKNOWN);
        assertThat(policy.hardFilterable(result)).isFalse();
    }

    @Test
    void missingEvidenceOrLowConfidenceIsNotHardFilterable() {
        assertThat(policy.hardFilterable(new PolicyGenderClassificationResult(
                PolicyGenderAudience.MALE_ONLY, true, 0.95, PolicyApplicantScope.INDIVIDUAL, ""))).isFalse();
        assertThat(policy.hardFilterable(new PolicyGenderClassificationResult(
                PolicyGenderAudience.MALE_ONLY, true, 0.4, PolicyApplicantScope.INDIVIDUAL, "남성 대상"))).isFalse();
    }

    @Test
    void organizationScopeSpecificGenderIsNotHardFilterableForPersonalEligibility() {
        PolicyGenderClassificationResult result = policy.normalize(new PolicyGenderClassificationResult(
                PolicyGenderAudience.FEMALE_ONLY, true, 0.9, PolicyApplicantScope.ORGANIZATION,
                "여성기업 대상"));

        assertThat(result.audience()).isEqualTo(PolicyGenderAudience.UNKNOWN);
        assertThat(policy.hardFilterable(result)).isFalse();
    }

    @Test
    void individualSpecificGenderWithEnoughConfidenceIsHardFilterable() {
        PolicyGenderClassificationResult result = new PolicyGenderClassificationResult(
                PolicyGenderAudience.FEMALE_ONLY, true, 0.9, PolicyApplicantScope.INDIVIDUAL,
                "개인 여성 구직자 대상");

        assertThat(policy.hardFilterable(result)).isTrue();
    }
}
