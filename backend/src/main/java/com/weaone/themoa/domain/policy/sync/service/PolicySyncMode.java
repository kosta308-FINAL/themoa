package com.weaone.themoa.domain.policy.sync.service;

public enum PolicySyncMode {
    INCREMENTAL(false),
    FULL_REINDEX(true);

    private final boolean forceEmbedding;

    PolicySyncMode(boolean forceEmbedding) {
        this.forceEmbedding = forceEmbedding;
    }

    public boolean forceEmbedding() {
        return forceEmbedding;
    }
}
