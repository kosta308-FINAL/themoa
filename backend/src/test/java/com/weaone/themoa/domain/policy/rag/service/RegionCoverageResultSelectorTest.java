package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.PolicySearchResultItem;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchCondition;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchMode;
import com.weaone.themoa.domain.policy.rag.dto.SearchQueryType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RegionCoverageResultSelectorTest {
    private final RegionCoverageResultSelector selector = new RegionCoverageResultSelector();

    @Test
    void regionPromotionDoesNotMoveNeedsConfirmationAheadOfPrimary() {
        PolicySearchResultItem nationwidePrimary = TestPolicyItems.item(1, "PRIMARY", "NATIONWIDE", 70.0);
        PolicySearchResultItem seoulNeedsConfirmation = TestPolicyItems.item(2, "NEEDS_CONFIRMATION", "EXACT_SIDO", 99.0);

        RegionCoverageResultSelector.Selection selection =
                selector.select(List.of(seoulNeedsConfirmation, nationwidePrimary), 0, 20, SearchQueryType.ELIGIBILITY_SEARCH);

        assertThat(selection.orderedResults()).extracting("policyId").containsExactly(1, 2);
    }

    @Test
    void regionPromotionStillWorksInsidePrimaryTier() {
        PolicySearchResultItem nationwidePrimary = TestPolicyItems.item(1, "PRIMARY", "NATIONWIDE", 90.0);
        PolicySearchResultItem seoulPrimary = TestPolicyItems.item(2, "PRIMARY", "EXACT_SIDO", 80.0);

        RegionCoverageResultSelector.Selection selection =
                selector.select(List.of(nationwidePrimary, seoulPrimary), 0, 20, SearchQueryType.ELIGIBILITY_SEARCH);

        assertThat(selection.orderedResults()).extracting("policyId").containsExactly(2, 1);
    }

    @Test
    void regionPromotionPlacesMultipleMatchingRegionsBeforeNationwide() {
        PolicySearchResultItem nationwidePrimary = TestPolicyItems.item(1, "PRIMARY", "NATIONWIDE", 95.0);
        PolicySearchResultItem multipleSigunguPrimary = TestPolicyItems.item(2, "PRIMARY", "MULTIPLE_SIGUNGU_MATCH", 80.0);
        PolicySearchResultItem multipleSidoPrimary = TestPolicyItems.item(3, "PRIMARY", "MULTIPLE_SIDO_MATCH", 85.0);

        RegionCoverageResultSelector.Selection selection =
                selector.select(List.of(nationwidePrimary, multipleSidoPrimary, multipleSigunguPrimary),
                        0, 20, SearchQueryType.ELIGIBILITY_SEARCH);

        assertThat(selection.orderedResults()).extracting("policyId").containsExactly(2, 3, 1);
    }

    @Test
    void regionExplicitGeneralSearchKeepsRankingOrderWithoutCoveragePromotion() {
        PolicySearchResultItem exactNeedsConfirmation = TestPolicyItems.item(1, "NEEDS_CONFIRMATION", "EXACT_SIDO", 70.0);
        PolicySearchResultItem nationwidePrimary = TestPolicyItems.item(2, "PRIMARY", "NATIONWIDE", 95.0);

        RegionCoverageResultSelector.Selection selection =
                selector.select(List.of(exactNeedsConfirmation, nationwidePrimary), 0, 20,
                        SearchQueryType.ELIGIBILITY_SEARCH, regionalCondition());

        assertThat(selection.orderedResults()).extracting("policyId").containsExactly(1, 2);
    }

    private PolicySearchCondition regionalCondition() {
        return new PolicySearchCondition("대구광역시", null, null, 22, null, null, null, "general",
                java.util.Set.of(), java.util.Set.of("청년"), java.util.Set.of("청년"), "대구", "RESOLVED", "SIDO",
                java.util.Set.of("대구광역시"), true, true, false, false, false, false, PolicySearchMode.HYBRID, 10);
    }
}
