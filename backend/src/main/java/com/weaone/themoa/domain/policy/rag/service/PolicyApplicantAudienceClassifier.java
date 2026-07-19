package com.weaone.themoa.domain.policy.rag.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weaone.themoa.domain.policy.policy.domain.Policy;
import com.weaone.themoa.domain.policy.policy.domain.PolicySearchProjection;
import com.weaone.themoa.domain.policy.policy.domain.PolicySourceSnapshot;
import com.weaone.themoa.domain.policy.policy.repository.PolicySearchProjectionRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicySourceSnapshotRepository;
import com.weaone.themoa.domain.policy.rag.dto.PolicyApplicantAudience;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class PolicyApplicantAudienceClassifier {
    private final PolicySearchProjectionRepository projectionRepository;
    private final PolicySourceSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    public PolicyApplicantAudienceClassifier() {
        this(null, null, new ObjectMapper());
    }

    public PolicyApplicantAudienceClassifier(PolicySearchProjectionRepository projectionRepository) {
        this(projectionRepository, null, new ObjectMapper());
    }

    @Autowired
    public PolicyApplicantAudienceClassifier(PolicySearchProjectionRepository projectionRepository,
                                             PolicySourceSnapshotRepository snapshotRepository,
                                             ObjectMapper objectMapper) {
        this.projectionRepository = projectionRepository;
        this.snapshotRepository = snapshotRepository;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    public PolicyApplicantAudience classify(Policy policy) {
        return classifyWithEvidence(policy).audience();
    }

    public ApplicantAudienceClassification classifyWithEvidence(Policy policy) {
        if (policy == null) {
            return new ApplicantAudienceClassification(PolicyApplicantAudience.UNKNOWN, List.of("정책 정보 없음"));
        }
        PolicySearchProjection projection = projectionRepository == null || policy.getId() == null
                ? null
                : projectionRepository.findByPolicyId(policy.getId()).orElse(null);
        PolicySourceSnapshot snapshot = snapshotRepository == null || policy.getId() == null
                ? null
                : snapshotRepository.findByPolicyId(policy.getId()).orElse(null);
        return classifyWithEvidence(policy, projection, snapshot);
    }

    public ApplicantAudienceClassification classifyWithEvidence(Policy policy,
                                                                PolicySearchProjection projection,
                                                                PolicySourceSnapshot snapshot) {
        String rawText = policyText(policy, projection, snapshot);
        String compactText = compact(rawText);
        if (!StringUtils.hasText(compactText)) {
            return new ApplicantAudienceClassification(PolicyApplicantAudience.UNKNOWN, List.of("신청 주체 판정 문구 없음"));
        }

        List<String> evidence = new ArrayList<>();
        boolean individualDirect = individualDirectSignal(compactText);
        boolean organizationSubject = organizationSubject(compactText);
        boolean organizationAction = organizationAction(compactText);
        boolean organizationBenefit = organizationBenefit(compactText);
        boolean organizationRecruitment = organizationRecruitment(compactText);
        boolean organizationApplication = organizationApplication(compactText);
        boolean organizationHiringSupport = organizationSubject && organizationAction && containsAny(compactText, "지원", "지급", "제공");
        boolean organizationOnly = organizationRecruitment
                || organizationApplication
                || (organizationSubject && organizationAction && organizationBenefit)
                || organizationHiringSupport;

        if (organizationSubject) evidence.add("기업 신청 주체 근거");
        if (organizationAction) evidence.add("기업 채용 행동 근거");
        if (organizationBenefit) evidence.add("기업 직접 수혜 근거");
        if (organizationRecruitment) evidence.add("참여/수요기업 모집 근거");
        if (organizationApplication) evidence.add("기업 신청 근거");
        if (individualDirect) evidence.add("개인 직접 신청/수혜 근거");

        if (organizationOnly && individualDirect) {
            evidence.add("기업과 개인 직접 근거가 모두 있어 MIXED로 판정");
            return new ApplicantAudienceClassification(PolicyApplicantAudience.MIXED, evidence);
        }
        if (organizationOnly) {
            evidence.add("강한 기업 신청/수혜 근거가 있고 개인 직접 신청 근거가 없어 ORGANIZATION_ONLY로 판정");
            return new ApplicantAudienceClassification(PolicyApplicantAudience.ORGANIZATION_ONLY, evidence);
        }
        if (individualDirect) {
            evidence.add("개인에게 직접 제공되는 정책으로 판정");
            return new ApplicantAudienceClassification(PolicyApplicantAudience.INDIVIDUAL, evidence);
        }
        return new ApplicantAudienceClassification(PolicyApplicantAudience.UNKNOWN,
                evidence.isEmpty() ? List.of("신청 주체 불명확") : evidence);
    }

    private boolean individualDirectSignal(String text) {
        return containsAny(text,
                "청년이직접신청", "구직자가신청", "개인신청", "본인이신청", "근로자본인",
                "중소기업에재직중인청년에게", "중소기업재직청년에게", "기업에취업한청년에게",
                "재직청년에게", "근로청년에게", "구직청년에게", "구직자에게",
                "청년에게수당지급", "청년에게지원금지급", "청년에게교육", "청년에게상담",
                "청년에게서비스제공", "청년에게제공", "청년에게지원");
    }

    private boolean organizationSubject(String text) {
        return containsAny(text, "기업", "벤처기업", "중소기업", "법인", "사업주", "사업장",
                "고용주", "참여기업", "수요기업");
    }

    private boolean organizationAction(String text) {
        return containsAny(text, "기업모집", "참여기업모집", "수요기업모집", "공고게시",
                "취업공고게시", "채용공고게시", "인재채용", "우수인재채용", "채용박람회",
                "채용수요", "구인", "인력확보", "채용신청", "청년을채용", "채용하면");
    }

    private boolean organizationBenefit(String text) {
        return containsAny(text, "기업의인재채용지원", "인재채용을지원", "인재채용지원",
                "채용을지원", "채용비지원", "채용비용지원", "인건비지원", "인건비지급",
                "고용장려금", "사업장지원", "기업운영비지원", "기업에인건비지급",
                "기업에지급", "기업지원");
    }

    private boolean organizationRecruitment(String text) {
        return containsAny(text, "참여기업모집", "수요기업모집", "기업모집", "참여기업으로신청",
                "수요기업을모집", "기업을모집");
    }

    private boolean organizationApplication(String text) {
        return containsAny(text, "기업이신청", "기업신청", "법인대상", "사업주지원", "사업장지원",
                "기업대상", "참여기업으로신청", "기업이받을수있는");
    }

    private String policyText(Policy policy, PolicySearchProjection projection, PolicySourceSnapshot snapshot) {
        String condition = policy == null || policy.getCondition() == null ? "" : join(
                policy.getCondition().getConditionSummary(),
                policy.getCondition().getIncomeCondition());
        return join(
                projection == null ? null : projection.getTitleText(),
                projection == null ? null : projection.getTargetText(),
                projection == null ? null : projection.getQualificationText(),
                projection == null ? null : projection.getApplicationText(),
                projection == null ? null : projection.getSupportText(),
                projection == null ? null : projection.getDescriptionText(),
                policy == null ? null : policy.getTitle(),
                policy == null ? null : policy.getSummary(),
                condition,
                snapshotText(snapshot));
    }

    private String snapshotText(PolicySourceSnapshot snapshot) {
        if (snapshot == null || !StringUtils.hasText(snapshot.getRawPolicyJson())) {
            return "";
        }
        try {
            Map<String, Object> fields = objectMapper.readValue(snapshot.getRawPolicyJson(), new TypeReference<>() {
            });
            return join(
                    text(fields, "plcyNm"),
                    text(fields, "ptcpPrpTrgtCn"),
                    text(fields, "addAplyQlfcCndCn"),
                    text(fields, "plcyAplyMthdCn"),
                    text(fields, "plcySprtCn"),
                    text(fields, "plcyExplnCn"));
        } catch (Exception ignored) {
            return snapshot.getRawPolicyJson();
        }
    }

    private String text(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private String join(String... values) {
        return Stream.of(values)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .reduce("", (left, right) -> left.isEmpty() ? right : left + " " + right);
    }

    private String compact(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "");
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    public record ApplicantAudienceClassification(PolicyApplicantAudience audience, List<String> evidence) {
        public ApplicantAudienceClassification {
            evidence = evidence == null ? List.of() : List.copyOf(evidence);
        }
    }
}
