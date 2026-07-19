package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.domain.Policy;
import com.weaone.themoa.domain.policy.policy.domain.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.domain.PolicySearchProjection;
import com.weaone.themoa.domain.policy.rag.dto.PolicyOfferingType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyOfferingTypeClassifierTest {
    private final PolicyOfferingTypeClassifier classifier = new PolicyOfferingTypeClassifier();

    @Test
    void startupFinanceIsNotHiddenByDirectBenefitWords() {
        assertThat(classifier.classify(projection("청년 창업자금 대출", "창업기업 보증 지원", "")).type())
                .isEqualTo(PolicyOfferingType.STARTUP_FINANCE);
    }

    @Test
    void jobSeekerAndEmployeeBenefitAreSeparated() {
        assertThat(classifier.classify(projection("취업날개 면접정장 지원", "", "")).type())
                .isEqualTo(PolicyOfferingType.JOB_SEEKER_SUPPORT);
        assertThat(classifier.classify(projection("재직자 복지 지원", "근로 청년 생활 지원", "")).type())
                .isEqualTo(PolicyOfferingType.EMPLOYEE_BENEFIT);
    }

    @Test
    void careerDevelopmentDetectsCertificateFee() {
        assertThat(classifier.classify(projection("자격증 응시료 지원", "", "")).type())
                .isEqualTo(PolicyOfferingType.CAREER_DEVELOPMENT);
    }

    private PolicySearchProjection projection(String title, String support, String qualification) {
        Policy policy = new Policy("P-" + title);
        ReflectionTestUtils.setField(policy, "id", Math.abs(title.hashCode()));
        policy.updateBasic(title, "기관", PolicyCategory.복지, title, null, null, null, true, true, "OPEN");
        PolicySearchProjection projection = new PolicySearchProjection(policy);
        projection.update(title, title, "", "복지", "", support, "", qualification, "", "기관",
                String.join(" ", title, support, qualification), "test", false);
        return projection;
    }
}
