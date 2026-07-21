package com.weaone.themoa.domain.policy.rag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.entity.PolicySearchProjection;
import com.weaone.themoa.domain.policy.policy.entity.PolicySource;
import com.weaone.themoa.domain.policy.policy.entity.PolicySourceSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyDisplayTitleResolverTest {
    private final PolicyDisplayTitleResolver resolver =
            new PolicyDisplayTitleResolver(new PolicyKeywordNormalizer(), new ObjectMapper());

    @Test
    void usesMoreCompleteProjectionTitle() {
        PolicySearchProjection projection = projection(1, "벤처기업 공동채용 지원사업");

        String title = resolver.resolve("벤처기업 공동채용 지원사", projection, null, "벤처기업 공동채용 지원사");

        assertThat(title).isEqualTo("벤처기업 공동채용 지원사업");
    }

    @Test
    void usesMoreCompleteSnapshotTitleWhenProjectionIsAlsoShort() {
        Policy policy = policy(1, "벤처기업 공동채용 지원사");
        PolicySearchProjection projection = projection(1, "벤처기업 공동채용 지원사");
        PolicySourceSnapshot snapshot = snapshot(policy, "{\"plcyNm\":\"벤처기업 공동채용 지원사업\"}");

        String title = resolver.resolve(policy.getTitle(), projection, snapshot, policy.getTitle());

        assertThat(title).isEqualTo("벤처기업 공동채용 지원사업");
    }

    @Test
    void doesNotGuessMissingSuffixWhenNoSourceHasCompleteTitle() {
        Policy policy = policy(1, "벤처기업 공동채용 지원사");
        PolicySearchProjection projection = projection(1, "벤처기업 공동채용 지원사");
        PolicySourceSnapshot snapshot = snapshot(policy, "{\"plcyNm\":\"벤처기업 공동채용 지원사\"}");

        String title = resolver.resolve(policy.getTitle(), projection, snapshot, policy.getTitle());

        assertThat(title).isEqualTo("벤처기업 공동채용 지원사");
    }

    @Test
    void keepsParenthesisAliasTitleWhenItIsMoreComplete() {
        PolicySearchProjection projection = projection(1, "K-패스(K패스)");

        String title = resolver.resolve("K-패스", projection, null, "K-패스");

        assertThat(title).isEqualTo("K-패스(K패스)");
    }

    private PolicySearchProjection projection(Integer id, String title) {
        Policy policy = policy(id, title);
        PolicySearchProjection projection = new PolicySearchProjection(policy);
        projection.update(title, title, "", "", "", "", "", "", "", "",
                title, "test", false);
        return projection;
    }

    private Policy policy(Integer id, String title) {
        Policy policy = new Policy("SRC-" + id);
        ReflectionTestUtils.setField(policy, "id", id);
        policy.updateBasic(title, "기관", PolicyCategory.일자리, "", null, null, null, true, true, "OPEN");
        return policy;
    }

    private PolicySourceSnapshot snapshot(Policy policy, String rawJson) {
        return new PolicySourceSnapshot(policy, null, PolicySource.YOUTH_CENTER.name(), policy.getSourcePolicyId(),
                rawJson, "hash");
    }
}
