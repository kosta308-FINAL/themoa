package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.domain.Policy;
import com.weaone.themoa.domain.policy.policy.domain.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.domain.PolicyCondition;
import com.weaone.themoa.domain.policy.rag.dto.PolicyApplicantAudience;
import com.weaone.themoa.domain.policy.rag.dto.UserApplicantType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyApplicantAudienceClassifierTest {
    private final PolicyApplicantAudienceClassifier policyClassifier = new PolicyApplicantAudienceClassifier();
    private final UserApplicantTypeDetector userDetector = new UserApplicantTypeDetector();

    @Test
    void detectsOrganizationOnlyPolicy() {
        Policy policy = policy("벤처기업 공동채용 지원사업",
                "채용수요가 있는 벤처기업들이 합동으로 취업포털 사이트에 취업 공고를 게시하고 "
                        + "채용박람회를 통해 벤처기업의 우수 인재채용을 지원");

        assertThat(policyClassifier.classify(policy)).isEqualTo(PolicyApplicantAudience.ORGANIZATION_ONLY);
    }

    @Test
    void detectsOrganizationOnlyFromActualJointHiringWording() {
        Policy policy = policy("벤처기업 공동채용 지원사",
                "채용수요가 있는 벤처기업들이 합동으로 취업포털 사이트에 취업 공고를 게시하고 "
                        + "채용박람회를 통해 벤처기업의 우수 인재채용을 지원");

        var result = policyClassifier.classifyWithEvidence(policy);

        assertThat(result.audience()).isEqualTo(PolicyApplicantAudience.ORGANIZATION_ONLY);
        assertThat(result.evidence()).contains("기업 신청 주체 근거", "기업 채용 행동 근거");
    }

    @Test
    void doesNotTreatYouthHiringExpressionAsIndividualByItself() {
        Policy policy = policy("청년 채용 인건비 지원",
                "중소기업이 청년을 채용하면 기업에 인건비 지급");

        assertThat(policyClassifier.classify(policy)).isEqualTo(PolicyApplicantAudience.ORGANIZATION_ONLY);
    }

    @Test
    void keepsEmployedYouthPolicyAsIndividual() {
        Policy policy = policy("중소기업 재직 청년 복지 지원",
                "중소기업에 재직 중인 청년에게 복지 포인트를 지원");

        assertThat(policyClassifier.classify(policy)).isEqualTo(PolicyApplicantAudience.INDIVIDUAL);
    }

    @Test
    void keepsYouthEmployedAtCompanyAsIndividual() {
        Policy policy = policy("중소기업 재직 청년 자산형성 지원",
                "중소기업에 재직 중인 청년에게 자산형성 지원금을 지급");

        assertThat(policyClassifier.classify(policy)).isEqualTo(PolicyApplicantAudience.INDIVIDUAL);
    }

    @Test
    void detectsUserApplicantType() {
        assertThat(userDetector.detect("서울 회사에 다니고 있지만 이직 지원 정책이 있을까?"))
                .isEqualTo(UserApplicantType.INDIVIDUAL);
        assertThat(userDetector.detect("우리 회사에서 청년을 채용하려고 하는데 기업 지원사업 알려줘"))
                .isEqualTo(UserApplicantType.ORGANIZATION);
    }

    private Policy policy(String title, String target) {
        Policy policy = new Policy(title);
        policy.updateBasic(title, "기관", PolicyCategory.일자리, target, null, null, null, true, true, "OPEN");
        policy.updateCondition(new PolicyCondition(null, null, null, null, null, target, true));
        return policy;
    }
}
