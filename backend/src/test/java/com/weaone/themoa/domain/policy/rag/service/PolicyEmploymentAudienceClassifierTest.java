package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.entity.PolicySearchProjection;
import com.weaone.themoa.domain.policy.policy.repository.PolicySearchProjectionRepository;
import com.weaone.themoa.domain.policy.rag.dto.PolicyEmploymentAudience;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PolicyEmploymentAudienceClassifierTest {
    private final PolicyEmploymentAudienceClassifier classifier =
            new PolicyEmploymentAudienceClassifier(mock(PolicySearchProjectionRepository.class));

    @Test
    void classifiesUnemployedOnlyPolicy() {
        PolicyEmploymentAudience result = classifier.classify(projection("지원 대상: 미취업 청년", ""));

        assertThat(result.allowedStatuses()).containsExactly(UserEmploymentStatus.UNEMPLOYED);
        assertThat(result.exclusive()).isTrue();
    }

    @Test
    void classifiesUnemployedOnlyFromQualification() {
        PolicyEmploymentAudience result = classifier.classify(projection("", "신청 자격: 현재 재직 중이지 않은 자"));

        assertThat(result.allowedStatuses()).containsExactly(UserEmploymentStatus.UNEMPLOYED);
        assertThat(result.exclusive()).isTrue();
    }

    @Test
    void classifiesEmployedOnlyPolicy() {
        PolicyEmploymentAudience result = classifier.classify(projection("지원 대상: 중소기업 재직 청년", ""));

        assertThat(result.allowedStatuses()).containsExactly(UserEmploymentStatus.EMPLOYED);
        assertThat(result.exclusive()).isTrue();
    }

    @Test
    void generalYouthIsNotExclusiveEmploymentAudience() {
        PolicyEmploymentAudience result = classifier.classify(projection("지원 대상: 19세~39세 청년 누구나", ""));

        assertThat(result.allowedStatuses()).containsExactly(UserEmploymentStatus.UNKNOWN);
        assertThat(result.exclusive()).isFalse();
    }

    @Test
    void explicitEmploymentIrrelevantAllowsBothStatuses() {
        PolicyEmploymentAudience result = classifier.classify(projection("지원 대상: 19세~39세 청년 누구나", "취업 여부 무관"));

        assertThat(result.allowedStatuses()).contains(UserEmploymentStatus.EMPLOYED, UserEmploymentStatus.UNEMPLOYED);
        assertThat(result.exclusive()).isFalse();
    }

    @Test
    void qualificationRestrictionBeatsGeneralTarget() {
        PolicyEmploymentAudience result = classifier.classify(projection("지원 대상: 19~39세 청년", "추가 자격: 현재 미취업자"));

        assertThat(result.allowedStatuses()).containsExactly(UserEmploymentStatus.UNEMPLOYED);
        assertThat(result.exclusive()).isTrue();
    }

    @Test
    void internshipTitleAloneDoesNotBecomeUnemployedOnly() {
        PolicyEmploymentAudience result = classifier.classify(projectionWithTitle("청년 인턴십", "지원 대상: 19세~39세 청년", ""));

        assertThat(result.allowedStatuses().size() == 1
                && result.allowedStatuses().contains(UserEmploymentStatus.UNEMPLOYED)).isFalse();
        assertThat(result.exclusive()).isFalse();
    }

    @Test
    void missingTargetIsUnknown() {
        PolicyEmploymentAudience result = classifier.classify(projection("", ""));

        assertThat(result.allowedStatuses()).containsExactly(UserEmploymentStatus.UNKNOWN);
        assertThat(result.exclusive()).isFalse();
    }

    private PolicySearchProjection projection(String target, String qualification) {
        return projectionWithTitle("테스트 정책", target, qualification);
    }

    private PolicySearchProjection projectionWithTitle(String title, String target, String qualification) {
        Policy policy = new Policy("P-EMP-AUD");
        ReflectionTestUtils.setField(policy, "id", 1);
        policy.updateBasic(title, "기관", PolicyCategory.일자리, "", null, null, null, true, true, "OPEN");
        PolicySearchProjection projection = new PolicySearchProjection(policy);
        projection.update(title, title, "", "일자리", "", "", target, qualification,
                "", "기관", target + " " + qualification, "test", false);
        return projection;
    }
}
