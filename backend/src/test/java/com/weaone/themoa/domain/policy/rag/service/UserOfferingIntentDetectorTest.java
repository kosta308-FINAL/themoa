package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.PolicyOfferingType;
import com.weaone.themoa.domain.policy.rag.dto.UserOfferingIntent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserOfferingIntentDetectorTest {
    private final UserOfferingIntentDetector detector = new UserOfferingIntentDetector();

    @Test
    void detectsBroadBenefitIntent() {
        UserOfferingIntent intent = detector.detect("직장인이 받을 수 있는 혜택");

        assertThat(intent.broadBenefitIntent()).isTrue();
        assertThat(intent.explicitProgramIntent()).isFalse();
        assertThat(intent.preferredTypes()).contains(PolicyOfferingType.DIRECT_BENEFIT, PolicyOfferingType.GENERAL_SERVICE);
    }

    @Test
    void detectsExplicitProgramIntent() {
        assertThat(detector.detect("인턴십 알려줘").preferredTypes()).contains(PolicyOfferingType.EMPLOYMENT_OPPORTUNITY);
        assertThat(detector.detect("해외봉사 프로그램 알려줘").preferredTypes()).contains(PolicyOfferingType.VOLUNTEER_PROGRAM);
    }

    @Test
    void broadPolicySearchHasNoOfferingType() {
        UserOfferingIntent intent = detector.detect("서울 청년 정책");

        assertThat(intent.broadBenefitIntent()).isFalse();
        assertThat(intent.explicitProgramIntent()).isFalse();
        assertThat(intent.preferredTypes()).isEmpty();
    }
}
