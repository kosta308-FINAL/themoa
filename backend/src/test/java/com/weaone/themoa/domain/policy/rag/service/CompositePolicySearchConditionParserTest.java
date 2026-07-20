package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.domain.RegionCode;
import com.weaone.themoa.domain.policy.policy.region.FakeRegionData;
import com.weaone.themoa.domain.policy.policy.region.RegionAliasCatalog;
import com.weaone.themoa.domain.policy.policy.region.RegionCatalog;
import com.weaone.themoa.domain.policy.policy.region.RegionNormalizer;
import com.weaone.themoa.domain.policy.policy.region.UserRegionTextResolver;
import com.weaone.themoa.domain.policy.policy.repository.RegionCodeRepository;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchCondition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompositePolicySearchConditionParserTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserRegionTextResolver regionResolver = resolver();
    private final UserEmploymentStatusDetector employmentDetector = new UserEmploymentStatusDetector();
    private final RuleBasedPolicySearchConditionParser ruleParser =
            new RuleBasedPolicySearchConditionParser(regionResolver, employmentDetector);
    private final PolicySearchConditionValidator validator = new PolicySearchConditionValidator(
            new ExplicitConditionDetector(regionResolver, employmentDetector),
            new PolicyKeywordExtractor(new PolicyKeywordSynonymCatalog(), new PolicyKeywordNormalizer()),
            regionResolver,
            employmentDetector);

    @Test
    void openAiPathKeepsRuleResidenceWorkplaceAndNumericConditions() {
        String query = "수원에 살고 서울로 출근하는 29살 직장인이야. 받을 수 있는 혜택 알려줘.";
        CompositePolicySearchConditionParser parser = parser(openAi(null, "서울특별시", null, null,
                null, "UNKNOWN", null));

        PolicySearchCondition parsed = parser.parse(query, 20).condition();

        assertThat(parsed.province()).isEqualTo("경기도");
        assertThat(parsed.city()).isEqualTo("수원시");
        assertThat(parsed.workplaceProvince()).isEqualTo("서울특별시");
        assertThat(parsed.age()).isEqualTo(29);
        assertThat(parsed.employmentStatus()).isEqualTo("EMPLOYED");
    }

    @Test
    void openAiPathKeepsHighSchoolInferredAgeAndUnemployedSlang() {
        CompositePolicySearchConditionParser highSchoolParser = parser(openAi(null, "경기도", null, null,
                null, "UNKNOWN", null));
        PolicySearchCondition highSchool = highSchoolParser.parse("경기도에 사는 고3인데 취업 관련 지원이 궁금해", 20).condition();

        assertThat(highSchool.province()).isEqualTo("경기도");
        assertThat(highSchool.ageExplicit()).isFalse();
        assertThat(highSchool.inferredAge()).isEqualTo(18);
        assertThat(highSchool.inferredAgeSource()).isEqualTo("고3");

        CompositePolicySearchConditionParser unemployedParser = parser(openAi(null, null, null, null,
                null, "UNKNOWN", null));
        PolicySearchCondition unemployed = unemployedParser.parse("나 지금 백수인데 서울에서 받을 수 있는 지원금 있어?", 20).condition();

        assertThat(unemployed.province()).isEqualTo("서울특별시");
        assertThat(unemployed.employmentStatus()).isEqualTo("UNEMPLOYED");
        assertThat(unemployed.employmentExplicit()).isTrue();
    }

    @Test
    void openAiPathKeepsResidenceOverWorkplaceForComplexRegionSentence() {
        CompositePolicySearchConditionParser parser = parser(openAi(null, "서울특별시", null, null,
                null, null, null));

        PolicySearchCondition condition = parser.parse("서울에서 일하고 있지만 사는 곳은 인천이야. 지역 지원 정책을 찾아줘.", 20).condition();

        assertThat(condition.province()).isEqualTo("인천광역시");
        assertThat(condition.workplaceProvince()).isEqualTo("서울특별시");
    }

    private CompositePolicySearchConditionParser parser(CompositePolicySearchConditionParser.OpenAiPolicySearchAnalysis analysis) {
        @SuppressWarnings("unchecked")
        ObjectProvider<ChatModel> provider = mock(ObjectProvider.class);
        ChatModel chatModel = mock(ChatModel.class);
        when(provider.getIfAvailable()).thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class))).thenReturn(response(analysis));
        return new CompositePolicySearchConditionParser(provider, ruleParser, validator,
                new PolicyIntentPolarityDetector(), "test-key");
    }

    private ChatResponse response(CompositePolicySearchConditionParser.OpenAiPolicySearchAnalysis analysis) {
        try {
            return new ChatResponse(List.of(new Generation(new AssistantMessage(objectMapper.writeValueAsString(analysis)))));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private CompositePolicySearchConditionParser.OpenAiPolicySearchAnalysis openAi(String rawRegionText,
                                                                                  String province,
                                                                                  String city,
                                                                                  String district,
                                                                                  Integer age,
                                                                                  String employmentStatus,
                                                                                  Boolean studentStatus) {
        return new CompositePolicySearchConditionParser.OpenAiPolicySearchAnalysis(rawRegionText, province, city, district,
                age, employmentStatus, studentStatus, null, null, Set.of(), Set.of("청년"),
                "청년 지원 정책", Set.of(), Set.of(), Set.of("청년"), Set.of(), false);
    }

    private UserRegionTextResolver resolver() {
        RegionCodeRepository repository = mock(RegionCodeRepository.class);
        List<RegionCode> regions = FakeRegionData.regions();
        when(repository.findAll()).thenReturn(regions);
        for (RegionCode region : regions) {
            when(repository.findByRegionCode(region.getRegionCode())).thenReturn(Optional.of(region));
            when(repository.findByProvince(region.getProvince())).thenReturn(regions.stream()
                    .filter(candidate -> candidate.getProvince().equals(region.getProvince())).toList());
            if (region.getCity() != null) {
                when(repository.findByProvinceAndCity(region.getProvince(), region.getCity())).thenReturn(regions.stream()
                        .filter(candidate -> candidate.getProvince().equals(region.getProvince()) && region.getCity().equals(candidate.getCity()))
                        .toList());
            }
        }
        RegionAliasCatalog aliases = new RegionAliasCatalog();
        RegionNormalizer normalizer = new RegionNormalizer(aliases);
        return new UserRegionTextResolver(new RegionCatalog(repository, aliases, normalizer), aliases, normalizer);
    }
}
