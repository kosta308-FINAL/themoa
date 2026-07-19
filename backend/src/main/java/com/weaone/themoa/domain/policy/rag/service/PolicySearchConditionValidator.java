package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.rag.dto.PolicySearchCondition;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchMode;
import com.weaone.themoa.domain.policy.policy.region.UserRegionContext;
import com.weaone.themoa.domain.policy.policy.region.UserRegionResolution;
import com.weaone.themoa.domain.policy.policy.region.UserRegionTextResolver;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatusResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

@Component
public class PolicySearchConditionValidator {
    private final ExplicitConditionDetector explicitDetector;
    private final PolicyKeywordExtractor keywordExtractor;
    private final UserRegionTextResolver userRegionTextResolver;
    private final UserEmploymentStatusDetector employmentStatusDetector;

    public PolicySearchConditionValidator(ExplicitConditionDetector explicitDetector,
                                          PolicyKeywordExtractor keywordExtractor,
                                          UserRegionTextResolver userRegionTextResolver,
                                          UserEmploymentStatusDetector employmentStatusDetector) {
        this.explicitDetector = explicitDetector;
        this.keywordExtractor = keywordExtractor;
        this.userRegionTextResolver = userRegionTextResolver;
        this.employmentStatusDetector = employmentStatusDetector;
    }

    public PolicySearchCondition validate(String query, PolicySearchCondition parsed, Integer resultSize) {
        UserRegionContext regionContext = userRegionTextResolver.resolveContext(query);
        UserRegionResolution resolvedRegion = resolveRegion(query, parsed, regionContext);
        boolean regionExplicit = resolvedRegion.resolved();
        boolean ageExplicit = explicitDetector.ageExplicit(query);
        UserEmploymentStatusResult detectedEmployment = employmentStatusDetector.detect(query);
        boolean employmentExplicit = detectedEmployment.explicit();
        boolean studentExplicit = explicitDetector.studentExplicit(query);
        boolean categoryExplicit = explicitDetector.categoryExplicit(query);
        boolean supportTypeExplicit = explicitDetector.supportTypeExplicit(query);
        PolicyKeywordExtractor.KeywordSet keywords = keywordExtractor.extract(query, parsed == null ? null : parsed.keywords());

        String province = regionExplicit ? resolvedRegion.province() : null;
        String city = regionExplicit ? resolvedRegion.city() : null;
        String district = regionExplicit ? resolvedRegion.district() : null;
        Integer age = ageExplicit && parsed != null ? parsed.age() : null;
        String employment = detectedEmployment.explicit()
                ? detectedEmployment.status().name()
                : employmentFromParsedWithEvidence(query, parsed);
        Boolean student = studentExplicit && parsed != null ? parsed.studentStatus() : null;
        String category = parsed == null ? null : parsed.category();
        Set<String> supportTypes = parsed == null ? java.util.Set.<String>of() : parsed.supportTypes();
        PolicySearchMode mode = mode(regionExplicit, ageExplicit, employmentExplicit, studentExplicit,
                StringUtils.hasText(category) || !supportTypes.isEmpty(), !keywords.coreKeywords().isEmpty());
        return new PolicySearchCondition(province, city, district, age, employment, student,
                parsed == null ? null : parsed.careerStage(), category, supportTypes, keywords.coreKeywords(),
                keywords.expandedKeywords(), resolvedRegion.regionName(), resolvedRegion.status().name(),
                resolvedRegion.regionLevel() == null ? null : resolvedRegion.regionLevel().name(),
                java.util.Set.copyOf(resolvedRegion.candidates()),
                regionExplicit, ageExplicit, employmentExplicit, studentExplicit,
                categoryExplicit, supportTypeExplicit, mode, resultSize,
                parsed == null ? null : parsed.inferredAge(),
                parsed == null ? null : parsed.inferredAgeSource(),
                parsed == null ? null : parsed.inferredMinimumAge(),
                parsed == null ? null : parsed.inferredMaximumAge(),
                workplace(regionContext, parsed).province(),
                workplace(regionContext, parsed).city(),
                workplace(regionContext, parsed).district(),
                workplace(regionContext, parsed).regionName(),
                workplace(regionContext, parsed).status().name());
    }

    private String employmentFromParsedWithEvidence(String query, PolicySearchCondition parsed) {
        if (parsed == null || !StringUtils.hasText(parsed.employmentStatus())) {
            return null;
        }
        UserEmploymentStatusResult detected = employmentStatusDetector.detect(query);
        if (detected.explicit()) {
            return detected.status().name();
        }
        return explicitDetector.employmentExplicit(query) ? parsed.employmentStatus() : null;
    }

    private UserRegionResolution resolveRegion(String query, PolicySearchCondition parsed, UserRegionContext regionContext) {
        UserRegionResolution residence = regionContext == null ? UserRegionResolution.notFound() : regionContext.residence();
        if (residence.resolved() || residence.status().name().equals("AMBIGUOUS")) {
            return residence;
        }
        UserRegionResolution workplace = regionContext == null ? UserRegionResolution.notFound() : regionContext.workplace();
        if (workplace.resolved()) {
            return UserRegionResolution.notFound();
        }
        if (parsed != null && StringUtils.hasText(parsed.rawRegionText())) {
            UserRegionResolution raw = userRegionTextResolver.resolve(parsed.rawRegionText());
            if (raw.resolved() || raw.status().name().equals("AMBIGUOUS")) {
                return raw;
            }
        }
        if (parsed != null && StringUtils.hasText(parsed.province())) {
            String combined = parsed.province() + " " + (parsed.city() == null ? "" : parsed.city());
            UserRegionResolution openAiRegion = userRegionTextResolver.resolve(combined);
            if (openAiRegion.resolved() || openAiRegion.status().name().equals("AMBIGUOUS")) {
                return openAiRegion;
            }
        }
        return UserRegionResolution.notFound();
    }

    private UserRegionResolution workplace(UserRegionContext context, PolicySearchCondition parsed) {
        UserRegionResolution fromContext = context == null ? UserRegionResolution.notFound() : context.workplace();
        if (fromContext.resolved()) {
            return fromContext;
        }
        if (parsed != null && StringUtils.hasText(parsed.workplaceRawRegionText())) {
            UserRegionResolution raw = userRegionTextResolver.resolve(parsed.workplaceRawRegionText());
            if (raw.resolved()) {
                return raw;
            }
        }
        return UserRegionResolution.notFound();
    }

    private PolicySearchMode mode(boolean regionExplicit,
                                  boolean ageExplicit,
                                  boolean employmentExplicit,
                                  boolean studentExplicit,
                                  boolean categoryOrSupportPresent,
                                  boolean keywordPresent) {
        boolean hasHardCondition = regionExplicit || ageExplicit || employmentExplicit || studentExplicit;
        if (hasHardCondition && keywordPresent) {
            return PolicySearchMode.HYBRID;
        }
        if (hasHardCondition || (categoryOrSupportPresent && !keywordPresent)) {
            return PolicySearchMode.CONDITION;
        }
        return PolicySearchMode.KEYWORD;
    }
}
