package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCondition;
import com.weaone.themoa.domain.policy.rag.dto.EligibilityBreadth;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyEligibilityBreadthClassifierTest {
    private final PolicyEligibilityBreadthClassifier classifier = new PolicyEligibilityBreadthClassifier();

    @Test
    void classifiesBroadModerateRestrictedAndHighlyRestrictedTargets() {
        assertThat(classifier.classify(policy("19~39세 청년 누구나")).breadth()).isEqualTo(EligibilityBreadth.BROAD);
        assertThat(classifier.classify(policy("대학생 대상")).breadth()).isEqualTo(EligibilityBreadth.MODERATE);
        assertThat(classifier.classify(policy("차상위계층 대상")).breadth()).isEqualTo(EligibilityBreadth.RESTRICTED);
        assertThat(classifier.classify(policy("생계급여 수급 가구")).breadth()).isEqualTo(EligibilityBreadth.HIGHLY_RESTRICTED);
        assertThat(classifier.classify(policy("청년예술인 대상")).breadth()).isEqualTo(EligibilityBreadth.RESTRICTED);
    }

    private Policy policy(String condition) {
        Policy policy = new Policy("P-" + condition.hashCode());
        ReflectionTestUtils.setField(policy, "id", Math.abs(condition.hashCode()));
        policy.updateBasic("정책", "기관", PolicyCategory.복지, "요약", null, null, null, true, true, "OPEN");
        policy.updateCondition(new PolicyCondition(19, 39, null, null, null, condition, false));
        return policy;
    }
}
