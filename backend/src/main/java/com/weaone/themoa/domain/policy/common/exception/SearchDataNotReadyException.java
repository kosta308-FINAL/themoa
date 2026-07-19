package com.weaone.themoa.domain.policy.common.exception;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.policy.admin.dto.SearchReadinessResponse;

public class SearchDataNotReadyException extends BusinessException {
    private final SearchReadinessResponse readiness;

    public SearchDataNotReadyException(SearchReadinessResponse readiness) {
        super(ErrorCode.POLICY_SEARCH_NOT_READY);
        this.readiness = readiness;
    }

    public SearchReadinessResponse readiness() {
        return readiness;
    }
}
