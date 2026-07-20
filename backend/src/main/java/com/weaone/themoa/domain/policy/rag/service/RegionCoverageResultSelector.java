package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.region.RegionCompatibility;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchResultItem;
import com.weaone.themoa.domain.policy.rag.dto.RecommendationTier;
import com.weaone.themoa.domain.policy.rag.dto.SearchQueryType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class RegionCoverageResultSelector {

    /**
     * 첫 페이지에서 지역 대표성을 보정하되 RecommendationTier 경계를 넘지 않는다.
     *
     * <p>지역 정확도가 높은 NEEDS_CONFIRMATION 정책이 전국 PRIMARY 정책보다 앞서면 사용자는 확인 필요
     * 정책을 조건에 잘 맞는 정책으로 오해한다. 따라서 PRIMARY 그룹과 NEEDS_CONFIRMATION 그룹을 먼저
     * 나누고, 각 그룹 안에서만 정확 시군·상위 시도·전국 대표 정책을 앞쪽에 배치한다.</p>
     */
    public Selection select(List<PolicySearchResultItem> sortedResults, int page, int size, SearchQueryType queryType) {
        if (page != 0 || sortedResults.isEmpty()
                || queryType == SearchQueryType.POLICY_NAME
                || queryType == SearchQueryType.TOPIC_SEARCH) {
            return new Selection(sortedResults, 0, 0, 0);
        }
        List<PolicySearchResultItem> reordered = new ArrayList<>();
        Counter counter = new Counter();
        appendTier(sortedResults, reordered, RecommendationTier.PRIMARY, counter);
        appendTier(sortedResults, reordered, RecommendationTier.NEEDS_CONFIRMATION, counter);
        appendTier(sortedResults, reordered, RecommendationTier.MISMATCH, counter);
        sortedResults.stream()
                .filter(item -> reordered.stream().noneMatch(existing -> existing.policyId().equals(item.policyId())))
                .forEach(reordered::add);
        int nationwideSelected = (int) reordered.stream()
                .limit(size)
                .filter(item -> RegionCompatibility.NATIONWIDE.name().equals(item.regionCompatibility()))
                .count();
        return new Selection(reordered, counter.exact, counter.parent, nationwideSelected);
    }

    private void appendTier(List<PolicySearchResultItem> sortedResults, List<PolicySearchResultItem> reordered,
                            RecommendationTier tier, Counter counter) {
        List<PolicySearchResultItem> tierItems = sortedResults.stream()
                .filter(item -> tier.name().equals(item.recommendationTier()))
                .toList();
        if (tierItems.isEmpty()) {
            return;
        }
        LinkedHashSet<Integer> promoted = new LinkedHashSet<>();
        counter.exact += promote(tierItems, promoted, RegionCompatibility.EXACT_SIGUNGU);
        int parent = promote(tierItems, promoted, RegionCompatibility.PARENT_SIDO);
        if (parent == 0) {
            parent = promote(tierItems, promoted, RegionCompatibility.EXACT_SIDO);
        }
        counter.parent += parent;
        promote(tierItems, promoted, RegionCompatibility.NATIONWIDE);
        promoted.forEach(id -> tierItems.stream()
                .filter(item -> item.policyId().equals(id))
                .findFirst()
                .ifPresent(reordered::add));
        tierItems.stream()
                .filter(item -> !promoted.contains(item.policyId()))
                .forEach(reordered::add);
    }

    private int promote(List<PolicySearchResultItem> results, LinkedHashSet<Integer> promoted, RegionCompatibility compatibility) {
        return results.stream()
                .filter(item -> compatibility.name().equals(item.regionCompatibility()))
                .max(Comparator.comparingDouble(PolicySearchResultItem::finalScore))
                .map(item -> {
                    promoted.add(item.policyId());
                    return 1;
                })
                .orElse(0);
    }

    private static class Counter {
        int exact;
        int parent;
    }

    public record Selection(List<PolicySearchResultItem> orderedResults,
                            int exactSigunguSelectedCount,
                            int parentSidoSelectedCount,
                            int nationwideSelectedCount) {
    }
}
