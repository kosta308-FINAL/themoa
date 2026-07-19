package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.CandidateSource;

import java.util.Set;

public record PolicyLexicalCandidate(
        Integer policyId,
        double lexicalScore,
        double titleScore,
        Set<CandidateSource> sources
) {
}
