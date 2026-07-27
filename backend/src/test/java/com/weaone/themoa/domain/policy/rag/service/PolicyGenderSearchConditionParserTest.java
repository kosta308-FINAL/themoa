package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.PolicySearchCondition;
import com.weaone.themoa.domain.policy.rag.dto.UserGender;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyGenderSearchConditionParserTest {

    @Test
    void openAiStructuredAnalysisCarriesExplicitMaleGender() {
        CompositePolicySearchConditionParser.OpenAiPolicySearchAnalysis analysis =
                analysis("MALE", true);

        PolicySearchCondition condition = analysis.toCondition(10);

        assertThat(condition.gender()).isEqualTo(UserGender.MALE);
        assertThat(condition.genderExplicit()).isTrue();
    }

    @Test
    void openAiStructuredAnalysisCarriesExplicitFemaleGender() {
        CompositePolicySearchConditionParser.OpenAiPolicySearchAnalysis analysis =
                analysis("FEMALE", true);

        PolicySearchCondition condition = analysis.toCondition(10);

        assertThat(condition.gender()).isEqualTo(UserGender.FEMALE);
        assertThat(condition.genderExplicit()).isTrue();
    }

    @Test
    void invalidOrImplicitGenderDoesNotBecomeSearchTargetGender() {
        PolicySearchCondition invalid = analysis("WOMAN", true).toCondition(10);
        PolicySearchCondition implicit = analysis("FEMALE", false).toCondition(10);

        assertThat(invalid.gender()).isNull();
        assertThat(invalid.genderExplicit()).isFalse();
        assertThat(implicit.gender()).isEqualTo(UserGender.FEMALE);
        assertThat(implicit.genderExplicit()).isFalse();
    }

    private CompositePolicySearchConditionParser.OpenAiPolicySearchAnalysis analysis(String gender,
                                                                                     boolean genderExplicit) {
        return new CompositePolicySearchConditionParser.OpenAiPolicySearchAnalysis(
                null,
                null,
                null,
                null,
                26,
                "UNEMPLOYED",
                null,
                null,
                "general",
                Set.of(),
                Set.of("청년"),
                gender,
                genderExplicit,
                "청년 정책",
                Set.of("GENERAL"),
                Set.of(),
                Set.of("청년"),
                Set.of(),
                false
        );
    }
}
