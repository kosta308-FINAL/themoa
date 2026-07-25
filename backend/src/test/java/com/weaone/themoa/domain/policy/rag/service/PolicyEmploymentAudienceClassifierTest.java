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
    void strongUnemployedEvidenceInDescriptionIsExclusiveWhenTargetFieldsAreMissing() {
        PolicyEmploymentAudience result = classifier.classify(
                projectionWithTexts("경기도 청년 면접수당", "", "", "구직 활동 중인 미취업 청년에게 면접비를 지원합니다.", "")
        );

        assertThat(result.allowedStatuses()).containsExactly(UserEmploymentStatus.UNEMPLOYED);
        assertThat(result.exclusive()).isTrue();
    }

    @Test
    void targetUnemployedBeatsPostParticipationWorkExperienceInDescription() {
        PolicyEmploymentAudience result = classifier.classify(
                projectionWithTexts(
                        "경기청년 맞춤형 채용지원 서비스",
                        "도내 미취업 청년 대상",
                        "미취업 상태의 청년",
                        "기업과 일자리 매칭 후 일정 기간 근로 경험 제공",
                        ""
                )
        );

        assertThat(result.allowedStatuses()).containsExactly(UserEmploymentStatus.UNEMPLOYED);
        assertThat(result.exclusive()).isTrue();
        assertThat(result.conflict()).isFalse();
    }

    @Test
    void conflictingEmploymentEvidenceIsNotBothAllowedWithoutExplicitExpression() {
        PolicyEmploymentAudience result = classifier.classify(
                projectionWithTexts(
                        "청년 지원",
                        "미취업 청년과 현재 중소기업에 재직 중인 청년 지원",
                        "",
                        "",
                        ""
                )
        );

        assertThat(result.allowedStatuses()).contains(UserEmploymentStatus.EMPLOYED, UserEmploymentStatus.UNEMPLOYED);
        assertThat(result.exclusive()).isFalse();
        assertThat(result.conflict()).isTrue();
    }

    @Test
    void jobSeekerAndDiscouragedYouthAreUnemployedExclusive() {
        PolicyEmploymentAudience jobSeeker = classifier.classify(projection("지원 대상: 구직 등록자", ""));
        PolicyEmploymentAudience discouraged = classifier.classify(projection("지원 대상: 구직 단념 청년", ""));

        assertThat(jobSeeker.allowedStatuses()).containsExactly(UserEmploymentStatus.UNEMPLOYED);
        assertThat(jobSeeker.exclusive()).isTrue();
        assertThat(discouraged.allowedStatuses()).containsExactly(UserEmploymentStatus.UNEMPLOYED);
        assertThat(discouraged.exclusive()).isTrue();
    }

    @Test
    void generalEmploymentTopicIsNotUnemployedExclusive() {
        PolicyEmploymentAudience jobMatching = classifier.classify(
                projectionWithTexts("청년 일자리 매칭 서비스", "지원 대상: 19세~39세 청년", "", "채용 정보와 직무 교육을 제공합니다.", "")
        );
        PolicyEmploymentAudience training = classifier.classify(
                projectionWithTexts("직무교육 프로그램", "지원 대상: 19세~39세 청년", "", "직업 훈련과 일경험을 제공합니다.", "")
        );

        assertThat(jobMatching.exclusive()).isFalse();
        assertThat(jobMatching.allowedStatuses()).containsExactly(UserEmploymentStatus.UNKNOWN);
        assertThat(training.exclusive()).isFalse();
        assertThat(training.allowedStatuses()).containsExactly(UserEmploymentStatus.UNKNOWN);
    }

    @Test
    void employmentTransitionProgramIsMarkedForAutomaticRecommendationSafety() {
        PolicyEmploymentAudience matching = classifier.classify(
                projectionWithTexts(
                        "경기청년 일자리 매치업 플러스",
                        "지원 대상: 19세~39세 청년",
                        "",
                        "취업 청년과 중소기업 간 일자리를 매칭하고 정규직으로 채용될 수 있도록 지원",
                        ""
                )
        );
        PolicyEmploymentAudience climateJob = classifier.classify(
                projectionWithTexts(
                        "기후미래직업 청년일자리 전문교육 지원",
                        "지원 대상: 19세~39세 청년",
                        "",
                        "현장경험을 제공하여 청년의 기후미래직업 취업역량 및 취업연계 성과 제고",
                        ""
                )
        );

        assertThat(matching.exclusive()).isFalse();
        assertThat(matching.employmentTransitionProgram()).isTrue();
        assertThat(climateJob.exclusive()).isFalse();
        assertThat(climateJob.employmentTransitionProgram()).isTrue();
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
        return projectionWithTexts(title, target, qualification, "", "");
    }

    private PolicySearchProjection projectionWithTexts(String title, String target, String qualification,
                                                       String description, String support) {
        Policy policy = new Policy("P-EMP-AUD");
        ReflectionTestUtils.setField(policy, "id", 1);
        policy.updateBasic(title, "기관", PolicyCategory.일자리, "", null, null, null, true, true, "OPEN");
        PolicySearchProjection projection = new PolicySearchProjection(policy);
        projection.update(title, title, "", "일자리", description, support, target, qualification,
                "", "기관", target + " " + qualification + " " + description + " " + support, "test", false);
        return projection;
    }
}
