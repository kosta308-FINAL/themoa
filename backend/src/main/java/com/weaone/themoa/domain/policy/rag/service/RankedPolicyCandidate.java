package com.weaone.themoa.domain.policy.rag.service;

public record RankedPolicyCandidate(
        EvaluatedPolicyCandidate candidate,
        PolicyRankingEvaluation ranking
) {
}
