package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.PolicySearchCondition;
import com.weaone.themoa.domain.policy.rag.dto.PolicyQuerySemantics;

public interface PolicySearchConditionParser {
    ParsedPolicySearchCondition parse(String query, Integer resultSize);

    record ParsedPolicySearchCondition(
            PolicySearchCondition condition,
            PolicyQuerySemantics semantics,
            String parserMode,
            boolean fallback,
            String fallbackReason
    ) {
        public ParsedPolicySearchCondition(PolicySearchCondition condition, String parserMode, boolean fallback,
                                           String fallbackReason) {
            this(condition, PolicyQuerySemantics.empty(), parserMode, fallback, fallbackReason);
        }
    }
}
