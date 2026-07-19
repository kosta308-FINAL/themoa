package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.region.UserRegionTextResolver;
import com.weaone.themoa.domain.policy.policy.region.UserRegionContext;
import com.weaone.themoa.domain.policy.policy.region.UserRegionResolution;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchCondition;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatusResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RuleBasedPolicySearchConditionParser {
    private static final Pattern AGE = Pattern.compile("(\\d{1,2})\\s*(살|세)");
    private static final Pattern HIGH_SCHOOL_THIRD_GRADE = Pattern.compile("고\\s*3|고3|고등학교\\s*3\\s*학년");
    private final UserRegionTextResolver userRegionTextResolver;
    private final UserEmploymentStatusDetector employmentStatusDetector;
    private final PolicyIntentPolarityDetector polarityDetector = new PolicyIntentPolarityDetector();

    public RuleBasedPolicySearchConditionParser(UserRegionTextResolver userRegionTextResolver,
                                                UserEmploymentStatusDetector employmentStatusDetector) {
        this.userRegionTextResolver = userRegionTextResolver;
        this.employmentStatusDetector = employmentStatusDetector;
    }

    public PolicySearchCondition parseCondition(String query, Integer resultSize) {
        String text = query == null ? "" : query;
        Set<String> keywords = new LinkedHashSet<>();
        Set<String> supportTypes = new LinkedHashSet<>();
        String province = null;
        String city = null;
        String rawRegionText = null;
        String regionResolutionStatus = null;
        UserRegionContext regionContext = userRegionTextResolver.resolveContext(text);
        UserRegionResolution region = regionContext.residence();
        if (region.resolved()) {
            province = region.province();
            city = region.city();
            rawRegionText = region.regionName();
            regionResolutionStatus = region.status().name();
        } else if (region.status().name().equals("AMBIGUOUS")) {
            regionResolutionStatus = region.status().name();
        }
        Integer age = null;
        Matcher matcher = AGE.matcher(text);
        if (matcher.find()) {
            age = Integer.parseInt(matcher.group(1));
        }
        Integer inferredAge = null;
        String inferredAgeSource = null;
        Integer inferredMinimumAge = null;
        Integer inferredMaximumAge = null;
        if (age == null && HIGH_SCHOOL_THIRD_GRADE.matcher(text).find()) {
            inferredAge = 18;
            inferredAgeSource = "고3";
            inferredMinimumAge = 17;
            inferredMaximumAge = 18;
        }
        UserEmploymentStatusResult detectedEmployment = employmentStatusDetector.detect(text);
        String employment = detectedEmployment.explicit() ? detectedEmployment.status().name() : null;
        Boolean student = null;
        if (containsAny(text, "대학생", "재학생", "휴학생", "고3", "고등학생", "고교생")) {
            student = true;
        }
        String category = null;
        PolicyIntentPolarityDetector.IntentPolarityResult polarity = polarityDetector.detect(text);
        String positiveText = polarity.excludedDomains().isEmpty()
                ? text
                : String.join(" ", polarity.positiveTerms());
        if (containsAny(positiveText, "월세", "주거")) category = "주거";
        if (polarity.desiredDomains().contains(com.weaone.themoa.domain.policy.rag.dto.SearchDomain.EMPLOYMENT)) category = "일자리";
        if (containsAny(positiveText, "교육", "훈련")) category = "교육";
        if (containsAny(positiveText, "자산", "저축", "계좌", "통장", "대출", "융자", "이자", "금융")) category = "금융";
        add(text, keywords, "청년", "청년");
        add(text, keywords, "지원금", "지원금", "청년");
        add(text, keywords, "수당", "수당", "청년");
        add(text, keywords, "보조금", "보조금", "청년");
        if (polarity.positiveTerms().contains("면접")) {
            keywords.add("면접");
        }
        add(text, keywords, "월세", "월세", "주거");
        add(text, keywords, "생활비", "생활비");
        add(text, keywords, "자산", "자산형성", "저축");
        add(text, keywords, "계좌", "계좌", "저축", "자산형성");
        add(text, keywords, "통장", "통장", "저축", "자산형성");
        add(text, keywords, "저축", "저축", "계좌", "통장", "자산형성");
        if (containsAny(text, "지원금", "금전적 혜택", "받을 수 있는 돈", "돈으로 지원", "현금 지원", "현금으로 주는 혜택",
                "수당", "보조금", "장려금")) {
            supportTypes.add("CASH");
            supportTypes.add("CASH_ASSISTANCE");
        }
        if (containsAny(text, "수당")) {
            supportTypes.add("ALLOWANCE");
        }
        if (containsAny(text, "보조금", "장려금")) {
            supportTypes.add("SUBSIDY");
        }
        if (keywords.isEmpty()) {
            keywords.add("청년");
        }
        return new PolicySearchCondition(province, city, null, age, employment, student, null, category,
                supportTypes, keywords, Set.of(), rawRegionText, regionResolutionStatus,
                region.regionLevel() == null ? null : region.regionLevel().name(),
                Set.copyOf(region.candidates()),
                false, false, false, false, false, false, null, resultSize,
                inferredAge, inferredAgeSource, inferredMinimumAge, inferredMaximumAge,
                regionContext.workplace().province(), regionContext.workplace().city(), regionContext.workplace().district(),
                regionContext.workplace().regionName(), regionContext.workplace().status().name());
    }

    private void add(String text, Set<String> target, String trigger, String... values) {
        if (text.contains(trigger)) {
            target.addAll(Set.of(values));
        }
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }
}
