package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.entity.PolicySearchProjection;

public interface PolicyGenderAiClient {
    PolicyGenderAiAnalysis analyze(PolicySearchProjection projection);
}
