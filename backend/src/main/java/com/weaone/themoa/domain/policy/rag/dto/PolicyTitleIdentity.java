package com.weaone.themoa.domain.policy.rag.dto;

import java.util.Set;

public record PolicyTitleIdentity(
        String displayTitle,
        String canonicalTitle,
        Set<String> aliases,
        Set<String> normalizedAliases
) {
    public PolicyTitleIdentity {
        aliases = aliases == null ? Set.of() : Set.copyOf(aliases);
        normalizedAliases = normalizedAliases == null ? Set.of() : Set.copyOf(normalizedAliases);
    }
}
