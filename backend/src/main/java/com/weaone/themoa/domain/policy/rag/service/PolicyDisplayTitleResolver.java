package com.weaone.themoa.domain.policy.rag.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weaone.themoa.domain.policy.policy.entity.PolicySearchProjection;
import com.weaone.themoa.domain.policy.policy.entity.PolicySourceSnapshot;
import com.weaone.themoa.domain.policy.rag.dto.PolicyTitleIdentity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class PolicyDisplayTitleResolver {
    private final PolicyKeywordNormalizer normalizer;
    private final ObjectMapper objectMapper;

    public PolicyDisplayTitleResolver(PolicyKeywordNormalizer normalizer) {
        this(normalizer, new ObjectMapper());
    }

    @Autowired
    public PolicyDisplayTitleResolver(PolicyKeywordNormalizer normalizer, ObjectMapper objectMapper) {
        this.normalizer = normalizer;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    public String resolve(String policyTitle, PolicySearchProjection projection) {
        return resolve(policyTitle, projection, null, policyTitle);
    }

    public String resolve(String policyTitle,
                          PolicySearchProjection projection,
                          PolicySourceSnapshot snapshot,
                          String resultItemTitle) {
        List<String> candidates = new ArrayList<>();
        candidates.add(snapshotTitle(snapshot));
        candidates.add(projection == null ? null : projection.getTitleText());
        candidates.add(policyTitle);
        candidates.add(resultItemTitle);

        String selected = null;
        for (String candidate : candidates) {
            if (!validTitle(candidate)) {
                continue;
            }
            if (!StringUtils.hasText(selected)) {
                selected = candidate.trim();
                continue;
            }
            if (moreComplete(candidate, selected) || aliasRelationship(selected, candidate)) {
                selected = candidate.trim();
            }
        }
        return StringUtils.hasText(selected) ? selected : "확인 필요";
    }

    private String snapshotTitle(PolicySourceSnapshot snapshot) {
        if (snapshot == null || !StringUtils.hasText(snapshot.getRawPolicyJson())) {
            return null;
        }
        try {
            Map<String, Object> fields = objectMapper.readValue(snapshot.getRawPolicyJson(), new TypeReference<>() {
            });
            return text(fields, "plcyNm", "title", "policyName");
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private String text(Map<String, Object> fields, String... keys) {
        for (String key : keys) {
            Object value = fields.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }

    private boolean validTitle(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = normalizer.normalize(value);
        return StringUtils.hasText(normalized)
                && !List.of("확인필요", "미정", "없음", "unknown", "null").contains(normalized);
    }

    private boolean moreComplete(String candidate, String current) {
        String normalizedCandidate = normalizer.normalize(candidate);
        String normalizedCurrent = normalizer.normalize(current);
        return normalizedCandidate.length() > normalizedCurrent.length()
                && normalizedCandidate.startsWith(normalizedCurrent);
    }

    private boolean aliasRelationship(String current, String candidate) {
        PolicyTitleIdentity identity = normalizer.titleIdentity(candidate);
        String normalizedCurrent = normalizer.normalize(current);
        return StringUtils.hasText(normalizedCurrent)
                && identity.normalizedAliases().contains(normalizedCurrent);
    }
}
