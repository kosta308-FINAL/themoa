package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.repository.PolicySearchProjectionRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PolicyLexicalIndexBuilder {
    private final PolicySearchProjectionRepository projectionRepository;
    private final PolicyKeywordNormalizer normalizer;
    private volatile PolicyLexicalIndex index;

    public PolicyLexicalIndexBuilder(PolicySearchProjectionRepository projectionRepository,
                                     PolicyKeywordNormalizer normalizer) {
        this.projectionRepository = projectionRepository;
        this.normalizer = normalizer;
    }

    public PolicyLexicalIndex current() {
        PolicyLexicalIndex cached = index;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (index == null) {
                index = new PolicyLexicalIndex(projectionRepository.findAllActive(), normalizer);
            }
            return index;
        }
    }

    public PolicyLexicalIndex refresh() {
        synchronized (this) {
            index = new PolicyLexicalIndex(projectionRepository.findAllActive(), normalizer);
            return index;
        }
    }

    public void invalidate() {
        synchronized (this) {
            index = null;
        }
    }

    public int cachedDocumentCount() {
        PolicyLexicalIndex cached = index;
        return cached == null ? 0 : cached.size();
    }

    public Instant cachedBuiltAt() {
        PolicyLexicalIndex cached = index;
        return cached == null ? null : cached.builtAt();
    }
}
