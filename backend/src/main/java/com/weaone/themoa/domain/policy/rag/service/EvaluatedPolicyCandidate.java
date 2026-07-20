package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.entity.Policy;

public record EvaluatedPolicyCandidate(
        Policy policy,
        CandidateEvidence candidateEvidence,
        PolicyEligibilityEvaluation eligibility
) {
}
