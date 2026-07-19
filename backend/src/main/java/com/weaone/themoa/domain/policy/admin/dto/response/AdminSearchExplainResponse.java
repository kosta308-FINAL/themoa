package com.weaone.themoa.domain.policy.admin.dto.response;

import com.fasterxml.jackson.annotation.JsonAnyGetter;

import java.util.Map;

public record AdminSearchExplainResponse(
        Map<String, Object> fields
) {
    @Override
    @JsonAnyGetter
    public Map<String, Object> fields() {
        return fields;
    }
}
