package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.ConditionMatchStatus;

public record EmploymentAudienceMatch(ConditionMatchStatus status, String reason) {
}
