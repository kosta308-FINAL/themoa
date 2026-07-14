package com.weaone.themoa.domain.cardtransaction.dto.response;

import com.weaone.themoa.domain.cardtransaction.service.SyncSummary;

public record SyncResponse(int created, int updated, int skipped, boolean locked) {

    public static SyncResponse from(SyncSummary summary) {
        return new SyncResponse(summary.created(), summary.updated(), summary.skipped(), summary.locked());
    }
}
