package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.PolicySearchCondition;
import com.weaone.themoa.domain.policy.rag.dto.PolicyQuerySemantics;
import com.weaone.themoa.domain.policy.rag.dto.SearchDomain;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CompositePolicySearchConditionParser implements PolicySearchConditionParser {
    private final ObjectProvider<ChatModel> openAiChatModelProvider;
    private final RuleBasedPolicySearchConditionParser ruleBasedParser;
    private final PolicySearchConditionValidator conditionValidator;
    private final PolicyIntentPolarityDetector polarityDetector;
    private final String openAiApiKey;

    public CompositePolicySearchConditionParser(@Qualifier("openAiChatModel") ObjectProvider<ChatModel> openAiChatModelProvider,
                                                RuleBasedPolicySearchConditionParser ruleBasedParser,
                                                PolicySearchConditionValidator conditionValidator,
                                                PolicyIntentPolarityDetector polarityDetector,
                                                @Value("${spring.ai.openai.api-key:}") String openAiApiKey) {
        this.openAiChatModelProvider = openAiChatModelProvider;
        this.ruleBasedParser = ruleBasedParser;
        this.conditionValidator = conditionValidator;
        this.polarityDetector = polarityDetector;
        this.openAiApiKey = openAiApiKey;
    }

    @Override
    public ParsedPolicySearchCondition parse(String query, Integer resultSize) {
        PolicySearchCondition ruleCondition = ruleBasedParser.parseCondition(query, resultSize);
        if (StringUtils.hasText(openAiApiKey)) {
            ChatModel openAiChatModel = openAiChatModelProvider.getIfAvailable();
            if (openAiChatModel != null) {
                try {
                    OpenAiPolicySearchAnalysis analysis = ChatClient.builder(openAiChatModel).build()
                            .prompt()
                            .system("""
                                    You extract structured Korean youth policy search conditions.
                                    Return only conditions the user explicitly states about themselves or their search.
                                    Do not infer age, region, employmentStatus, or studentStatus from policy topic words.
                                    "청년 면접 수당" means keywords only: age=null, employmentStatus=null, province=null.
                                    "취업 지원 정책" or "면접 수당" does not mean the user is unemployed.
                                    "청년 정책" does not mean age=19.
                                    Set missing conditions to null, never to 0, false, or an empty string.
                                    Do not recommend or invent policies.
                                    Include rawRegionText when the user wrote a region phrase such as a province alias,
                                    official municipality name, or province-and-municipality phrase.
                                    Also separate positive search intent from negative/exclusion intent.
                                    A mentioned word is not necessarily a desired topic.
                                    Detect negation, exclusion, and lack-of-interest expressions.
                                    These exclude EMPLOYMENT: "취업 생각은 없어", "취업에는 관심 없어", "취업 정책은 필요 없어",
                                    "취업 말고", "취업 정책 제외", "일자리 지원은 빼줘", "구직 관련 정책은 원하지 않아".
                                    These may not mean employmentStatus. Current employment status and whether the user wants
                                    employment policies are separate.
                                    Positive EMPLOYMENT examples: "취업 지원을 받고 싶어", "취업 정책을 찾아줘",
                                    "구직 중이라 지원이 필요해", "면접비 지원을 찾고 있어", "취업 준비 중이야".
                                    UNEMPLOYED plus positive EMPLOYMENT examples: "미취업이라 취업 지원 정책을 찾고 있어",
                                    "직장이 없어서 구직 지원이 필요해", "취준생인데 면접비 지원을 찾고 있어".
                                    EMPLOYED status examples: "직장에 다니고 있다", "회사에서 일하고 있다",
                                    "현재 근무 중이다", "재직 중이다", "직장인", "회사원", "근로자".
                                    UNEMPLOYED status examples: "직장을 구하고 있다", "직장을 찾고 있다",
                                    "취업 준비 중이다", "현재 무직이다", "직장이 없다".
                                    "직장에 다니고 있다" means employmentStatus=EMPLOYED but not desiredDomains=EMPLOYMENT.
                                    "직장에 다니며 이직 지원을 찾고 있다" means employmentStatus=EMPLOYED and desiredDomains may include EMPLOYMENT.
                                    Return normalizedGoal with only positive search purpose. Do not include excluded topic words
                                    in normalizedGoal.
                                    Fields: rawRegionText, province, city, district, age, employmentStatus, studentStatus,
                                    careerStage, category, supportTypes, keywords, resultSize,
                                    normalizedGoal, desiredDomains, excludedDomains, positiveKeywords, excludedKeywords,
                                    explicitExclusion.
                                    employmentStatus examples: UNEMPLOYED, EMPLOYED.
                                    supportTypes examples: CASH, ALLOWANCE, SUBSIDY, HOUSING, EDUCATION.
                                    desiredDomains/excludedDomains examples: EMPLOYMENT, HOUSING, EDUCATION, WELFARE,
                                    FINANCE, STARTUP, CULTURE, HEALTH, CARE, GENERAL.
                                    """)
                            .user(query)
                            .call()
                            .entity(OpenAiPolicySearchAnalysis.class);
                    if (analysis != null) {
                        PolicySearchCondition condition = mergeConditions(query, ruleCondition, analysis.toCondition(resultSize), resultSize);
                        PolicyQuerySemantics openAiSemantics = analysis.toSemantics();
                        PolicyQuerySemantics semantics = polarityDetector.merge(query, openAiSemantics);
                        return new ParsedPolicySearchCondition(conditionValidator.validate(query, condition, resultSize),
                                semantics, "OPENAI:RULE_MERGED", false, null);
                    }
                } catch (RuntimeException ex) {
                    PolicyQuerySemantics semantics = polarityDetector.merge(query, null);
                    return new ParsedPolicySearchCondition(conditionValidator.validate(query, ruleCondition, resultSize),
                            semantics, "RULE_BASED", true, ex.getMessage());
                }
            }
        }
        return new ParsedPolicySearchCondition(conditionValidator.validate(query, ruleCondition, resultSize),
                polarityDetector.merge(query, null), "RULE_BASED", true,
                "OpenAI ChatModel is not configured.");
    }

    private PolicySearchCondition mergeConditions(String query,
                                                  PolicySearchCondition rule,
                                                  PolicySearchCondition openAi,
                                                  Integer resultSize) {
        if (openAi == null) {
            return rule;
        }
        boolean ruleHasResidence = StringUtils.hasText(rule.province()) || StringUtils.hasText(rule.city());
        boolean ruleHasWorkplace = StringUtils.hasText(rule.workplaceProvince()) || StringUtils.hasText(rule.workplaceCity());
        String province = ruleHasResidence ? rule.province() : (ruleHasWorkplace ? null : coalesce(openAi.province(), rule.province()));
        String city = ruleHasResidence ? rule.city() : (ruleHasWorkplace ? null : coalesce(openAi.city(), rule.city()));
        String district = ruleHasResidence ? rule.district() : (ruleHasWorkplace ? null : coalesce(openAi.district(), rule.district()));
        String rawRegionText = ruleHasResidence ? rule.rawRegionText() : (ruleHasWorkplace ? null : coalesce(openAi.rawRegionText(), rule.rawRegionText()));
        String regionResolutionStatus = ruleHasResidence ? rule.regionResolutionStatus() : (ruleHasWorkplace ? null : coalesce(openAi.regionResolutionStatus(), rule.regionResolutionStatus()));
        String regionLevel = ruleHasResidence ? rule.regionLevel() : (ruleHasWorkplace ? null : coalesce(openAi.regionLevel(), rule.regionLevel()));
        java.util.Set<String> regionCandidates = ruleHasResidence ? rule.regionCandidates() : openAi.regionCandidates();

        Integer age = rule.age() != null ? rule.age() : openAi.age();
        String employment = StringUtils.hasText(rule.employmentStatus())
                && !"UNKNOWN".equalsIgnoreCase(rule.employmentStatus())
                ? rule.employmentStatus()
                : openAi.employmentStatus();
        Boolean student = rule.studentStatus() != null ? rule.studentStatus() : openAi.studentStatus();
        return new PolicySearchCondition(
                province,
                city,
                district,
                age,
                employment,
                student,
                coalesce(openAi.careerStage(), rule.careerStage()),
                coalesce(openAi.category(), rule.category()),
                union(rule.supportTypes(), openAi.supportTypes()),
                union(rule.keywords(), openAi.keywords()),
                union(rule.expandedKeywords(), openAi.expandedKeywords()),
                rawRegionText,
                regionResolutionStatus,
                regionLevel,
                regionCandidates,
                rule.regionExplicit() || openAi.regionExplicit(),
                rule.ageExplicit() || openAi.ageExplicit(),
                rule.employmentExplicit() || openAi.employmentExplicit(),
                rule.studentExplicit() || openAi.studentExplicit(),
                rule.categoryExplicit() || openAi.categoryExplicit(),
                rule.supportTypeExplicit() || openAi.supportTypeExplicit(),
                openAi.searchMode() == null ? rule.searchMode() : openAi.searchMode(),
                resultSize,
                rule.inferredAge() != null ? rule.inferredAge() : openAi.inferredAge(),
                coalesce(rule.inferredAgeSource(), openAi.inferredAgeSource()),
                rule.inferredMinimumAge() != null ? rule.inferredMinimumAge() : openAi.inferredMinimumAge(),
                rule.inferredMaximumAge() != null ? rule.inferredMaximumAge() : openAi.inferredMaximumAge(),
                coalesce(rule.workplaceProvince(), openAi.workplaceProvince()),
                coalesce(rule.workplaceCity(), openAi.workplaceCity()),
                coalesce(rule.workplaceDistrict(), openAi.workplaceDistrict()),
                coalesce(rule.workplaceRawRegionText(), openAi.workplaceRawRegionText()),
                coalesce(rule.workplaceRegionResolutionStatus(), openAi.workplaceRegionResolutionStatus())
        );
    }

    private String coalesce(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred : fallback;
    }

    private java.util.Set<String> union(java.util.Set<String> first, java.util.Set<String> second) {
        java.util.LinkedHashSet<String> values = new java.util.LinkedHashSet<>();
        if (first != null) values.addAll(first);
        if (second != null) values.addAll(second);
        return java.util.Set.copyOf(values);
    }

    record OpenAiPolicySearchAnalysis(
            String rawRegionText,
            String province,
            String city,
            String district,
            Integer age,
            String employmentStatus,
            Boolean studentStatus,
            String careerStage,
            String category,
            java.util.Set<String> supportTypes,
            java.util.Set<String> keywords,
            String normalizedGoal,
            java.util.Set<String> desiredDomains,
            java.util.Set<String> excludedDomains,
            java.util.Set<String> positiveKeywords,
            java.util.Set<String> excludedKeywords,
            Boolean explicitExclusion
    ) {
        PolicySearchCondition toCondition(Integer resultSize) {
            return new PolicySearchCondition(province, city, district, age, employmentStatus, studentStatus,
                    careerStage, category, supportTypes, keywords, java.util.Set.of(), rawRegionText, null, null,
                    java.util.Set.of(), false, false, false, false, false, false, null, resultSize);
        }

        PolicyQuerySemantics toSemantics() {
            return new PolicyQuerySemantics(normalizedGoal,
                    domains(desiredDomains), domains(excludedDomains), positiveKeywords, excludedKeywords,
                    Boolean.TRUE.equals(explicitExclusion));
        }

        private java.util.Set<SearchDomain> domains(java.util.Set<String> raw) {
            if (raw == null) return java.util.Set.of();
            return raw.stream().map(SearchDomain::fromRaw)
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        }
    }
}
