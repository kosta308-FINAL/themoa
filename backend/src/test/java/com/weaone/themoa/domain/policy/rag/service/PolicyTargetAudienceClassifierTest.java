package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.domain.Policy;
import com.weaone.themoa.domain.policy.policy.domain.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.domain.PolicySearchProjection;
import com.weaone.themoa.domain.policy.policy.repository.PolicySearchProjectionRepository;
import com.weaone.themoa.domain.policy.rag.dto.EducationStage;
import com.weaone.themoa.domain.policy.rag.dto.PolicyTargetAudienceClassification;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PolicyTargetAudienceClassifierTest {
    private final PolicyTargetAudienceClassifier classifier = new PolicyTargetAudienceClassifier(mock(PolicySearchProjectionRepository.class));

    @Test
    void highSchoolTargetIsExclusive() {
        PolicyTargetAudienceClassification result = classifier.classify(projection("지원 대상: 고교생", "", ""));

        assertThat(result.includedStages()).containsExactly(EducationStage.HIGH_SCHOOL);
        assertThat(result.stageExclusive()).isTrue();
    }

    @Test
    void universityTargetIsExclusive() {
        PolicyTargetAudienceClassification result = classifier.classify(projection("지원 대상: 대학생 및 휴학생", "", ""));

        assertThat(result.includedStages()).contains(EducationStage.UNIVERSITY);
        assertThat(result.stageExclusive()).isTrue();
    }

    @Test
    void mixedHighSchoolAndUniversityKeepsBothStages() {
        PolicyTargetAudienceClassification result = classifier.classify(projection("지원 대상: 고등학생 및 대학생", "", ""));

        assertThat(result.includedStages()).contains(EducationStage.HIGH_SCHOOL, EducationStage.UNIVERSITY);
    }

    @Test
    void generalYouthIsNotStageExclusive() {
        PolicyTargetAudienceClassification result = classifier.classify(projection("지원 대상: 19세~39세 청년", "", ""));

        assertThat(result.includedStages()).contains(EducationStage.GENERAL_YOUTH);
        assertThat(result.stageExclusive()).isFalse();
    }

    @Test
    void missingTargetIsUnknown() {
        PolicyTargetAudienceClassification result = classifier.classify(projection("", "", ""));

        assertThat(result.includedStages()).containsExactly(EducationStage.UNKNOWN);
    }

    private PolicySearchProjection projection(String target, String qualification, String title) {
        Policy policy = new Policy("P-TARGET");
        ReflectionTestUtils.setField(policy, "id", 1);
        policy.updateBasic(title == null || title.isBlank() ? "테스트 정책" : title,
                "기관", PolicyCategory.일자리, "", null, null, null, true, true, "OPEN");
        PolicySearchProjection projection = new PolicySearchProjection(policy);
        projection.update("테스트정책", title, "", "일자리", "", "", target, qualification,
                "", "기관", String.join(" ", List.of(title, target, qualification)), "test", false);
        return projection;
    }
}
