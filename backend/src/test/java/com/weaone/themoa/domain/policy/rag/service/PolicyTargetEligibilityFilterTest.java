package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.ConditionMatchStatus;
import com.weaone.themoa.domain.policy.rag.dto.EducationStage;
import com.weaone.themoa.domain.policy.rag.dto.PolicyTargetAudienceClassification;
import com.weaone.themoa.domain.policy.rag.dto.TargetStageMatchResult;
import com.weaone.themoa.domain.policy.rag.dto.UserEducationStageCondition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyTargetEligibilityFilterTest {
    private final PolicyTargetEligibilityFilter filter = new PolicyTargetEligibilityFilter();
    private final UserEducationStageCondition university = new UserEducationStageCondition(Set.of(EducationStage.UNIVERSITY), true, List.of("대학생"));

    @Test
    void universityUserMismatchesHighSchoolOnlyPolicy() {
        TargetStageMatchResult result = filter.match(university, target(Set.of(EducationStage.HIGH_SCHOOL), true));

        assertThat(result.status()).isEqualTo(ConditionMatchStatus.MISMATCH);
    }

    @Test
    void universityUserMatchesUniversityPolicy() {
        TargetStageMatchResult result = filter.match(university, target(Set.of(EducationStage.UNIVERSITY), true));

        assertThat(result.status()).isEqualTo(ConditionMatchStatus.MATCH);
    }

    @Test
    void universityUserMatchesGeneralYouthPolicy() {
        TargetStageMatchResult result = filter.match(university, target(Set.of(EducationStage.GENERAL_YOUTH), false));

        assertThat(result.status()).isEqualTo(ConditionMatchStatus.MATCH);
    }

    @Test
    void universityUserMatchesMixedHighSchoolUniversityPolicy() {
        TargetStageMatchResult result = filter.match(university, target(Set.of(EducationStage.HIGH_SCHOOL, EducationStage.UNIVERSITY), true));

        assertThat(result.status()).isEqualTo(ConditionMatchStatus.MATCH);
    }

    @Test
    void unknownPolicyTargetIsKeptAsUnknown() {
        TargetStageMatchResult result = filter.match(university, target(Set.of(EducationStage.UNKNOWN), false));

        assertThat(result.status()).isEqualTo(ConditionMatchStatus.UNKNOWN);
    }

    private PolicyTargetAudienceClassification target(Set<EducationStage> stages, boolean exclusive) {
        return new PolicyTargetAudienceClassification(stages, Set.of(), exclusive, 0.9, List.of("test"));
    }
}
