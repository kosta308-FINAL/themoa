package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.region.RegionCompatibility;
import com.weaone.themoa.domain.policy.policy.region.RegionMatchEvaluator;
import com.weaone.themoa.domain.policy.policy.region.RegionMatchResult;
import com.weaone.themoa.domain.policy.policy.region.ResolvedUserRegion;
import com.weaone.themoa.domain.policy.rag.config.RagProperties;
import com.weaone.themoa.domain.policy.rag.dto.ConditionMatchResult;
import com.weaone.themoa.domain.policy.rag.dto.ConditionMatchStatus;
import com.weaone.themoa.domain.policy.rag.dto.PolicyApplicantAudience;
import com.weaone.themoa.domain.policy.rag.dto.PolicyEmploymentAudience;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchCondition;
import com.weaone.themoa.domain.policy.rag.dto.PolicySearchPlan;
import com.weaone.themoa.domain.policy.rag.dto.PolicyTargetAudienceClassification;
import com.weaone.themoa.domain.policy.rag.dto.RecommendationTier;
import com.weaone.themoa.domain.policy.rag.dto.SearchQueryType;
import com.weaone.themoa.domain.policy.rag.dto.TargetStageMatchResult;
import com.weaone.themoa.domain.policy.rag.dto.UserApplicantType;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatus;
import com.weaone.themoa.domain.policy.rag.dto.UserEmploymentStatusResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 정책 후보가 사용자의 명시 조건을 만족하는지 평가하는 서비스다.
 *
 * <p>후보 수집 이후, 랭킹 이전에 호출된다. 지역·나이·취업 상태처럼 명확한 자격 불일치는
 * 점수를 감점하는 대신 이 단계에서 제거한다. 불확실한 조건은 제거하지 않고 확인 필요 조건으로 남긴다.</p>
 *
 * <p>입력은 SearchPlan, 후보 정책, 사용자 지역, 대상/취업 audience 판정 결과이며 출력은
 * passedCandidates와 filter metrics다. 이 클래스에서는 Vector 점수나 최종 검색 순위를 계산하지 않는다.
 * DB 또는 외부 시스템을 직접 호출하지 않으며, 새 검색 점수를 추가하려면 PolicyRankingService를 수정해야 한다.</p>
 *
 * <p>취업 자격 Hard Filter는 PolicyEmploymentAudienceClassifier 결과를 단일 기준으로 사용한다.
 * 수집 데이터의 PolicyCondition.employmentStatus와 studentStatus Boolean은 오래된 구조와 관리자 참고용으로
 * 남겨두되, 정교한 대상/자격 Classifier 결과를 덮어쓰지 않는다. UNKNOWN은 통과도 불일치도 아니므로 확인
 * 필요 상태로만 보존한다.</p>
 */
@Service
public class PolicyEligibilityEvaluator {
    static final String REGION_NOT_MATCHED = "REGION_NOT_MATCHED";
    static final String AGE_NOT_MATCHED = "AGE_NOT_MATCHED";
    static final String EMPLOYMENT_NOT_MATCHED = "EMPLOYMENT_NOT_MATCHED";
    static final String STUDENT_NOT_MATCHED = "STUDENT_NOT_MATCHED";
    static final String APPLICANT_AUDIENCE_MISMATCH = "APPLICANT_AUDIENCE_MISMATCH";

    private final RagProperties properties;
    private final RegionMatchEvaluator regionMatchEvaluator;
    private final PolicyTargetEligibilityFilter targetEligibilityFilter;
    private final PolicyApplicantAudienceClassifier applicantAudienceClassifier;
    private final UserApplicantTypeDetector userApplicantTypeDetector;

    public PolicyEligibilityEvaluator(RagProperties properties,
                                      RegionMatchEvaluator regionMatchEvaluator,
                                      PolicyTargetEligibilityFilter targetEligibilityFilter) {
        this(properties, regionMatchEvaluator, targetEligibilityFilter,
                new PolicyApplicantAudienceClassifier(), new UserApplicantTypeDetector());
    }

    @Autowired
    public PolicyEligibilityEvaluator(RagProperties properties,
                                      RegionMatchEvaluator regionMatchEvaluator,
                                      PolicyTargetEligibilityFilter targetEligibilityFilter,
                                      PolicyApplicantAudienceClassifier applicantAudienceClassifier,
                                      UserApplicantTypeDetector userApplicantTypeDetector) {
        this.properties = properties;
        this.regionMatchEvaluator = regionMatchEvaluator;
        this.targetEligibilityFilter = targetEligibilityFilter;
        this.applicantAudienceClassifier = applicantAudienceClassifier;
        this.userApplicantTypeDetector = userApplicantTypeDetector;
    }

    public PolicyEvaluationResult evaluate(PolicySearchExecutionContext context,
                                           PolicyCandidateCollection candidates,
                                           ResolvedUserRegion userRegion,
                                           Map<Integer, PolicyTargetAudienceClassification> targetAudienceByPolicyId,
                                           Map<Integer, PolicyEmploymentAudience> employmentAudienceByPolicyId,
                                           UserEmploymentStatusResult userEmploymentStatus) {
        PolicySearchFilterMetrics metrics = new PolicySearchFilterMetrics();
        List<EvaluatedPolicyCandidate> passed = new ArrayList<>();
        Map<Integer, PolicyEligibilityEvaluation> evaluations = new java.util.LinkedHashMap<>();
        for (Policy policy : candidates.policies()) {
            CandidateEvidence evidence = candidates.evidenceByPolicyId()
                    .getOrDefault(policy.getId(), new CandidateEvidence(policy.getId(), List.of(), 0.0, 0.0, 0.0));
            PolicyEligibilityEvaluation evaluation = evaluateOne(context.plan(), policy, userRegion,
                    targetAudienceByPolicyId.getOrDefault(policy.getId(), PolicyTargetAudienceClassification.unknown()),
                    employmentAudienceByPolicyId.getOrDefault(policy.getId(), PolicyEmploymentAudience.unknown()),
                    userEmploymentStatus,
                    metrics);
            if (evaluation.passed()) {
                passed.add(new EvaluatedPolicyCandidate(policy, evidence, evaluation));
            }
            evaluations.put(policy.getId(), evaluation);
        }
        metrics.eligibilityPassedCandidateCount = passed.size();
        return new PolicyEvaluationResult(passed, evaluations, metrics);
    }

    private PolicyEligibilityEvaluation evaluateOne(PolicySearchPlan plan,
                                                    Policy policy,
                                                    ResolvedUserRegion userRegion,
                                                    PolicyTargetAudienceClassification targetAudience,
                                                    PolicyEmploymentAudience employmentAudience,
                                                    UserEmploymentStatusResult userEmploymentStatus,
                                                    PolicySearchFilterMetrics metrics) {
        PolicySearchCondition condition = plan.condition();
        List<String> matched = new ArrayList<>();
        List<String> needCheck = new ArrayList<>();
        RegionMatchResult regionMatch = regionMatchEvaluator.evaluate(policy, userRegion);
        countRegion(regionMatch.compatibility(), metrics);
        if (regionMatch.eligible()) metrics.regionEligibleCount++;
        else metrics.regionIneligibleCount++;
        if (condition.regionExplicit() && regionMatch.compatibility() == RegionCompatibility.NOT_MATCHED) {
            needCheck.add(REGION_NOT_MATCHED);
        } else if (condition.regionExplicit() && regionMatch.compatibility() == RegionCompatibility.UNKNOWN) {
            // 지역 UNKNOWN은 수집/분류 품질의 빈칸일 수 있으므로 설정에 따라 제외하고, 기본 원칙은 확인 필요로 보존한다.
            needCheck.add("REGION_UNKNOWN");
        } else {
            matched.add(regionMatch.reason());
        }

        ApplicantAudienceMatch applicantAudienceMatch = applicantAudienceMatch(plan, policy);
        if (applicantAudienceMatch.status() == ConditionMatchStatus.MISMATCH) {
            needCheck.add(APPLICANT_AUDIENCE_MISMATCH);
        } else if (applicantAudienceMatch.status() == ConditionMatchStatus.MATCH) {
            matched.add("신청 주체 일치: " + applicantAudienceMatch.reason());
        }

        ConditionMatchResult ageMatch = ageMatch(policy, condition);
        ConditionMatchResult employmentMatch = employmentMatch(policy, condition);
        ConditionMatchResult studentMatch = studentMatch(policy, condition);
        TargetStageMatchResult targetStageMatch = targetEligibilityFilter.match(plan, targetAudience);
        applyCondition("나이", ageMatch, condition.ageExplicit(), matched, needCheck, metrics);
        applyCondition("취업 상태", employmentMatch, condition.employmentExplicit(), matched, needCheck, metrics);
        applyCondition("학생 상태", studentMatch, condition.studentExplicit(), matched, needCheck, metrics);
        if (plan.educationStageExplicit()) {
            applyTargetStage(targetStageMatch, matched, needCheck, metrics);
        } else if (targetAudience.stageExclusive()) {
            // 교육 단계를 밝히지 않은 사용자는 대학생/고교생 전용 정책과 불일치한다고 단정할 수 없다.
            needCheck.add("대상 교육 단계 확인 필요: 사용자가 해당 교육 단계 소속을 밝히지 않았습니다.");
        }

        EmploymentAudienceMatch employmentAudienceMatch = employmentAudienceMatch(userEmploymentStatus, employmentAudience);
        if (employmentAudienceMatch.status() == ConditionMatchStatus.MISMATCH) {
            needCheck.add("EMPLOYMENT_AUDIENCE_NOT_MATCHED");
        } else if (employmentAudienceMatch.status() == ConditionMatchStatus.UNKNOWN) {
            needCheck.add("취업 대상 확인 필요: " + employmentAudienceMatch.reason());
        } else {
            matched.add("취업 대상 일치: " + employmentAudienceMatch.reason());
        }

        Recommendation recommendation = recommendation(plan, targetAudience, targetStageMatch, employmentAudience, employmentAudienceMatch);
        String excludedReason = excludedReason(policy, condition, needCheck, metrics);
        boolean passed = excludedReason == null;
        if (passed) {
            countRecommendation(recommendation.tier(), metrics);
        } else if ("EMPLOYMENT_AUDIENCE_NOT_MATCHED".equals(excludedReason)) {
            countEmploymentAudienceMismatch(userEmploymentStatus, metrics);
        }
        return new PolicyEligibilityEvaluation(policy.getId(), passed, regionMatch, ageMatch, employmentMatch,
                studentMatch, targetStageMatch, employmentAudienceMatch, recommendation.tier(), recommendation.reason(),
                matched, needCheck, excludedReason);
    }

    private String excludedReason(Policy policy, PolicySearchCondition condition, List<String> needCheck,
                                  PolicySearchFilterMetrics metrics) {
        if ("CLOSED".equals(policy.getStatus())) {
            metrics.applicationFiltered++;
            return "APPLICATION_CLOSED";
        }
        if (condition.regionExplicit() && needCheck.contains(REGION_NOT_MATCHED)) {
            metrics.regionFiltered++;
            metrics.regionHardFilteredCount++;
            metrics.wrongRegionExcludedCount++;
            return REGION_NOT_MATCHED;
        }
        if (condition.regionExplicit() && needCheck.contains("REGION_UNKNOWN")
                && !properties.getSearch().isIncludeUnknownRegion()) {
            metrics.regionFiltered++;
            metrics.unknownExcludedCount++;
            return "REGION_UNKNOWN";
        }
        if (condition.ageExplicit() && needCheck.contains(AGE_NOT_MATCHED)) {
            metrics.ageFiltered++;
            return AGE_NOT_MATCHED;
        }
        if (condition.employmentExplicit() && needCheck.contains(EMPLOYMENT_NOT_MATCHED)) {
            metrics.employmentFiltered++;
            return EMPLOYMENT_NOT_MATCHED;
        }
        if (needCheck.contains("EMPLOYMENT_AUDIENCE_NOT_MATCHED")) {
            metrics.employmentFiltered++;
            return "EMPLOYMENT_AUDIENCE_NOT_MATCHED";
        }
        if (needCheck.contains(APPLICANT_AUDIENCE_MISMATCH)) {
            metrics.targetFiltered++;
            return APPLICANT_AUDIENCE_MISMATCH;
        }
        if (needCheck.contains("TARGET_STAGE_NOT_MATCHED")) {
            metrics.targetFiltered++;
            return "TARGET_STAGE_NOT_MATCHED";
        }
        return null;
    }

    ConditionMatchResult ageMatch(Policy policy, PolicySearchCondition condition) {
        if (!condition.ageExplicit() || condition.age() == null || policy.getCondition() == null) {
            return ConditionMatchResult.unknown("나이 조건을 검색 필터로 사용하지 않았습니다.");
        }
        Integer min = normalizeAgeBound(policy.getCondition().getMinAge());
        Integer max = normalizeAgeBound(policy.getCondition().getMaxAge());
        if (min == null && max == null) {
            return ConditionMatchResult.unknown("정책 나이 조건 정보가 없습니다.");
        }
        if ((min == null || condition.age() >= min) && (max == null || condition.age() <= max)) {
            return ConditionMatchResult.match("사용자 " + condition.age() + "세가 정책 나이 범위에 포함됩니다.");
        }
        return ConditionMatchResult.mismatch("사용자 " + condition.age() + "세가 정책 나이 범위를 벗어납니다.");
    }

    private Integer normalizeAgeBound(Integer value) {
        return value == null || value <= 0 ? null : value;
    }

    ConditionMatchResult employmentMatch(Policy policy, PolicySearchCondition condition) {
        if (!condition.employmentExplicit() || !StringUtils.hasText(condition.employmentStatus())) {
            return ConditionMatchResult.unknown("취업 상태를 검색 필터로 사용하지 않았습니다.");
        }
        return ConditionMatchResult.unknown("정책 취업 자격은 대상/자격 텍스트 Classifier 결과로 판정합니다.");
    }

    ConditionMatchResult studentMatch(Policy policy, PolicySearchCondition condition) {
        if (!condition.studentExplicit() || condition.studentStatus() == null) {
            return ConditionMatchResult.unknown("학생 상태 조건 정보가 없거나 검색 필터로 사용하지 않았습니다.");
        }
        return ConditionMatchResult.unknown("학생 자격은 교육 단계 Classifier 결과로 판정합니다.");
    }

    private void applyCondition(String label, ConditionMatchResult result, boolean explicit,
                                List<String> matched, List<String> needCheck, PolicySearchFilterMetrics metrics) {
        if (!explicit) return;
        if ("나이".equals(label)) countAge(result.status(), metrics);
        if ("취업 상태".equals(label)) countEmployment(result.status(), metrics);
        if (result.status() == ConditionMatchStatus.MATCH) {
            matched.add(label + " 조건 일치: " + result.reason());
        } else if (result.status() == ConditionMatchStatus.UNKNOWN) {
            // 정책 조건 UNKNOWN은 데이터 결측일 수 있으므로 자동 제거하지 않고 확인 필요로 남긴다.
            needCheck.add(label + " 확인 필요: " + result.reason());
        } else if ("나이".equals(label)) {
            needCheck.add(AGE_NOT_MATCHED);
        } else if ("취업 상태".equals(label)) {
            needCheck.add(EMPLOYMENT_NOT_MATCHED);
        } else if ("학생 상태".equals(label)) {
            needCheck.add(STUDENT_NOT_MATCHED);
        }
    }

    private void applyTargetStage(TargetStageMatchResult result, List<String> matched,
                                  List<String> needCheck, PolicySearchFilterMetrics metrics) {
        if (result.status() == ConditionMatchStatus.MATCH) {
            matched.add("대상 교육 단계 일치: " + result.reason());
        } else if (result.status() == ConditionMatchStatus.MISMATCH) {
            needCheck.add("TARGET_STAGE_NOT_MATCHED");
        } else {
            metrics.targetUnknownCount++;
            needCheck.add("대상 교육 단계 확인 필요: " + result.reason());
        }
    }

    private EmploymentAudienceMatch employmentAudienceMatch(UserEmploymentStatusResult user, PolicyEmploymentAudience audience) {
        if (user == null || !user.explicit() || user.status() == UserEmploymentStatus.UNKNOWN) {
            return new EmploymentAudienceMatch(ConditionMatchStatus.UNKNOWN, "사용자 취업 상태가 명시되지 않았습니다.");
        }
        if (audience == null || audience.allowedStatuses().contains(UserEmploymentStatus.UNKNOWN)) {
            return new EmploymentAudienceMatch(ConditionMatchStatus.UNKNOWN, "정책 취업 대상 상태 확인 필요");
        }
        if (audience.allowedStatuses().contains(user.status())) {
            return new EmploymentAudienceMatch(ConditionMatchStatus.MATCH, "사용자 취업 상태가 정책 대상에 포함됩니다.");
        }
        if (audience.exclusive()) {
            String reason = user.status() == UserEmploymentStatus.EMPLOYED
                    ? "정책 대상이 미취업자로 제한되어 현재 재직 상태와 일치하지 않습니다."
                    : "정책 대상이 재직자로 제한되어 현재 미취업 상태와 일치하지 않습니다.";
            return new EmploymentAudienceMatch(ConditionMatchStatus.MISMATCH, reason);
        }
        return new EmploymentAudienceMatch(ConditionMatchStatus.UNKNOWN, "정책 취업 대상 상태가 배타적인지 확인 필요");
    }

    private ApplicantAudienceMatch applicantAudienceMatch(PolicySearchPlan plan, Policy policy) {
        if (plan.queryType() == SearchQueryType.POLICY_NAME) {
            return new ApplicantAudienceMatch(ConditionMatchStatus.MATCH, "정책명 정확 검색은 신청 주체 필터를 우회합니다.");
        }
        UserApplicantType userType = userApplicantType(plan);
        PolicyApplicantAudienceClassifier.ApplicantAudienceClassification classification =
                applicantAudienceClassifier.classifyWithEvidence(policy);
        PolicyApplicantAudience policyAudience = classification.audience();
        String evidence = String.join(", ", classification.evidence());
        if (userType == UserApplicantType.INDIVIDUAL && policyAudience == PolicyApplicantAudience.ORGANIZATION_ONLY) {
            return new ApplicantAudienceMatch(ConditionMatchStatus.MISMATCH,
                    "개인 사용자 검색에서 기업 신청 전용 정책입니다. evidence=" + evidence);
        }
        if (userType == UserApplicantType.ORGANIZATION && policyAudience == PolicyApplicantAudience.ORGANIZATION_ONLY) {
            return new ApplicantAudienceMatch(ConditionMatchStatus.MATCH,
                    "기업 사용자 검색에서 기업 신청 정책을 허용합니다. evidence=" + evidence);
        }
        if (policyAudience == PolicyApplicantAudience.MIXED || policyAudience == PolicyApplicantAudience.INDIVIDUAL) {
            return new ApplicantAudienceMatch(ConditionMatchStatus.MATCH, "개인 신청 가능 정책입니다. evidence=" + evidence);
        }
        return new ApplicantAudienceMatch(ConditionMatchStatus.UNKNOWN, "신청 주체 정보 확인 필요");
    }

    private UserApplicantType userApplicantType(PolicySearchPlan plan) {
        UserApplicantType detected = userApplicantTypeDetector.detect(plan.originalQuery());
        if (detected != UserApplicantType.UNKNOWN) {
            return detected;
        }
        PolicySearchCondition condition = plan.condition();
        if (condition != null && (condition.ageExplicit() || condition.regionExplicit()
                || condition.employmentExplicit() || condition.studentExplicit())) {
            return UserApplicantType.INDIVIDUAL;
        }
        return UserApplicantType.UNKNOWN;
    }

    private Recommendation recommendation(PolicySearchPlan plan,
                                          PolicyTargetAudienceClassification targetAudience,
                                          TargetStageMatchResult targetStageMatch,
                                          PolicyEmploymentAudience employmentAudience,
                                          EmploymentAudienceMatch employmentAudienceMatch) {
        if (targetStageMatch.status() == ConditionMatchStatus.MISMATCH
                || employmentAudienceMatch.status() == ConditionMatchStatus.MISMATCH) {
            return new Recommendation(RecommendationTier.MISMATCH, "명시 조건과 정책 대상이 불일치합니다.");
        }
        if (!plan.educationStageExplicit()
                && targetAudience.stageExclusive()
                && targetAudience.includedStages().stream().anyMatch(stage -> switch (stage) {
            case ELEMENTARY, MIDDLE_SCHOOL, HIGH_SCHOOL, UNIVERSITY, GRADUATE_SCHOOL -> true;
            default -> false;
        })) {
            return new Recommendation(RecommendationTier.NEEDS_CONFIRMATION,
                    "학생 여부가 확인되지 않아 전용 교육 단계 자격을 추가로 확인해야 합니다.");
        }
        if (employmentAudienceMatch.status() == ConditionMatchStatus.UNKNOWN && employmentAudience.exclusive()) {
            return new Recommendation(RecommendationTier.NEEDS_CONFIRMATION, "정책 취업 대상 상태 확인이 필요합니다.");
        }
        return new Recommendation(RecommendationTier.PRIMARY, "사용자의 명시 조건과 일치합니다.");
    }

    private void countRegion(RegionCompatibility compatibility, PolicySearchFilterMetrics metrics) {
        switch (compatibility) {
            case EXACT_SIGUNGU -> {
                metrics.exactSigunguMatchedCount++;
                metrics.cityMatchedCount++;
            }
            case EXACT_SIDO -> {
                metrics.exactSidoMatchedCount++;
                metrics.provinceMatchedCount++;
            }
            case PARENT_SIDO -> {
                metrics.parentSidoMatchedCount++;
                metrics.provinceMatchedCount++;
            }
            case CHILD_SIGUNGU_MATCH -> {
                metrics.exactSigunguMatchedCount++;
                metrics.cityMatchedCount++;
            }
            case NATIONWIDE -> {
                metrics.nationwideMatchedCount++;
                metrics.nationwideCandidateCount++;
            }
            case MULTIPLE_REGION_MATCH, MULTIPLE_SIGUNGU_MATCH, MULTIPLE_CHILD_SIGUNGU_MATCH, MULTIPLE_SIDO_MATCH -> metrics.multipleRegionMatchedCount++;
            case REGION_UNSPECIFIED, UNKNOWN -> metrics.regionUnknownCount++;
            case NOT_MATCHED -> metrics.regionNotMatchedCount++;
        }
    }

    private void countAge(ConditionMatchStatus status, PolicySearchFilterMetrics metrics) {
        switch (status) {
            case MATCH -> metrics.ageMatchedCount++;
            case UNKNOWN -> metrics.ageUnknownCount++;
            case MISMATCH -> metrics.ageMismatchedCount++;
        }
    }

    private void countEmployment(ConditionMatchStatus status, PolicySearchFilterMetrics metrics) {
        switch (status) {
            case MATCH -> metrics.employmentMatchedCount++;
            case UNKNOWN -> metrics.employmentUnknownCount++;
            case MISMATCH -> metrics.employmentMismatchedCount++;
        }
    }

    private void countRecommendation(RecommendationTier tier, PolicySearchFilterMetrics metrics) {
        if (tier == RecommendationTier.PRIMARY) metrics.primaryCandidateCount++;
        if (tier == RecommendationTier.NEEDS_CONFIRMATION) metrics.needsConfirmationCandidateCount++;
    }

    private void countEmploymentAudienceMismatch(UserEmploymentStatusResult userEmploymentStatus, PolicySearchFilterMetrics metrics) {
        if (userEmploymentStatus == null) {
            return;
        }
        if (userEmploymentStatus.status() == UserEmploymentStatus.EMPLOYED) {
            metrics.employedMismatchFiltered++;
        } else if (userEmploymentStatus.status() == UserEmploymentStatus.UNEMPLOYED) {
            metrics.unemployedMismatchFiltered++;
        }
    }

    private boolean employmentSoftSignal(Policy policy) {
        String text = String.join(" ",
                nullToEmpty(policy.getTitle()),
                nullToEmpty(policy.getSummary()),
                policy.getCondition() == null ? "" : nullToEmpty(policy.getCondition().getConditionSummary()));
        return containsAny(text, "구직자", "미취업 청년", "취업 준비", "취업준비", "면접", "구직활동", "취업지원", "일자리");
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) return true;
        }
        return false;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record Recommendation(RecommendationTier tier, String reason) {
    }

    private record ApplicantAudienceMatch(ConditionMatchStatus status, String reason) {
    }
}
