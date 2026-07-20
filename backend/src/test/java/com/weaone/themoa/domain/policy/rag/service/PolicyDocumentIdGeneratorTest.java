package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyDocumentIdGeneratorTest {
    @Test
    void generatesDeterministicIdFromSourcePolicyId() {
        PolicyDocumentIdGenerator generator = new PolicyDocumentIdGenerator();
        Policy policy = new Policy("P001");
        policy.updateBasic("청년 지원", "경기도", PolicyCategory.일자리, "지원", null, null, null, true, true, "OPEN");

        assertThat(generator.documentId(policy)).isEqualTo(generator.documentId(policy));
    }
}
