package com.weaone.themoa.domain.policy.rag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.entity.PolicySearchProjection;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicySearchProjectionRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicySourceSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PolicySearchProjectionServiceTest {

    private final PolicyRepository policyRepository = mock(PolicyRepository.class);
    private final PolicySourceSnapshotRepository snapshotRepository = mock(PolicySourceSnapshotRepository.class);
    private final PolicySearchProjectionRepository projectionRepository = mock(PolicySearchProjectionRepository.class);
    private final PolicyLexicalIndexBuilder lexicalIndexBuilder = mock(PolicyLexicalIndexBuilder.class);
    private final PolicySearchProjectionService service = new PolicySearchProjectionService(
            policyRepository,
            snapshotRepository,
            projectionRepository,
            new PolicyKeywordNormalizer(),
            new ObjectMapper(),
            mock(TransactionTemplate.class),
            lexicalIndexBuilder
    );

    @Test
    void rebuildBatchLoadsExistingProjectionsInBatch() {
        Policy first = policy(1);
        Policy second = policy(2);
        when(policyRepository.findWithRelationsByIdIn(List.of(1, 2))).thenReturn(List.of(first, second));
        when(snapshotRepository.findByPolicyIdIn(List.of(1, 2))).thenReturn(List.of());
        when(projectionRepository.findByPolicyIdIn(List.of(1, 2))).thenReturn(List.of());

        service.rebuildBatch(List.of(1, 2));

        verify(projectionRepository).findByPolicyIdIn(List.of(1, 2));
        verify(projectionRepository, never()).findByPolicyId(1);
        verify(projectionRepository, never()).findByPolicyId(2);
        verify(projectionRepository).saveAll(anyList());
    }

    private Policy policy(Integer id) {
        Policy policy = new Policy("YC-" + id);
        ReflectionTestUtils.setField(policy, "id", id);
        policy.updateBasic("정책 " + id, "기관", PolicyCategory.일자리, "요약",
                "https://example.com/" + id, LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 31), false, true, "신청중");
        return policy;
    }
}
