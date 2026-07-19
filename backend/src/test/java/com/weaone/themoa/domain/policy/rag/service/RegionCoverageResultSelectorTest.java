package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.SearchQueryType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegionCoverageResultSelectorTest {
    private final RegionCoverageResultSelector selector = new RegionCoverageResultSelector();

    @Test
    void regionPromotionDoesNotMoveNeedsConfirmationAheadOfPrimary() {
        var nationwidePrimary = TestPolicyItems.item(1, "PRIMARY", "NATIONWIDE", 70.0);
        var seoulNeedsConfirmation = TestPolicyItems.item(2, "NEEDS_CONFIRMATION", "EXACT_SIDO", 99.0);

        var selection = selector.select(java.util.List.of(seoulNeedsConfirmation, nationwidePrimary), 0, 20, SearchQueryType.ELIGIBILITY_SEARCH);

        assertThat(selection.orderedResults()).extracting("policyId").containsExactly(1, 2);
    }

    @Test
    void regionPromotionStillWorksInsidePrimaryTier() {
        var nationwidePrimary = TestPolicyItems.item(1, "PRIMARY", "NATIONWIDE", 90.0);
        var seoulPrimary = TestPolicyItems.item(2, "PRIMARY", "EXACT_SIDO", 80.0);

        var selection = selector.select(java.util.List.of(nationwidePrimary, seoulPrimary), 0, 20, SearchQueryType.ELIGIBILITY_SEARCH);

        assertThat(selection.orderedResults()).extracting("policyId").containsExactly(2, 1);
    }
}
