package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicySearchProjection;
import com.weaone.themoa.domain.policy.policy.repository.PolicySearchProjectionRepository;
import com.weaone.themoa.domain.policy.rag.dto.EligibilityBreadth;
import com.weaone.themoa.domain.policy.rag.dto.PolicyEligibilityBreadthClassification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 정책의 대상·자격 문구를 기준으로 범용성을 분류한다.
 *
 * <p>제목이나 설명의 단어 하나로 특수 대상을 확정하지 않고, targetText와 qualificationText,
 * 신청 자격 성격의 문구를 우선한다. 범용성은 Hard Filter가 아니라 ranking/사용자 표시용 신호이므로,
 * 불확실한 경우 UNKNOWN으로 둔다.</p>
 */
@Component
public class PolicyEligibilityBreadthClassifier {
    private final PolicySearchProjectionRepository projectionRepository;

    public PolicyEligibilityBreadthClassifier() {
        this.projectionRepository = null;
    }

    @Autowired
    public PolicyEligibilityBreadthClassifier(PolicySearchProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    public PolicyEligibilityBreadthClassification classify(Policy policy) {
        PolicySearchProjection projection = projectionRepository == null || policy == null || policy.getId() == null
                ? null
                : projectionRepository.findByPolicyId(policy.getId()).orElse(null);
        if (projection != null) {
            return classify(projection);
        }
        String text = policy == null || policy.getCondition() == null ? "" : nullToEmpty(policy.getCondition().getConditionSummary());
        return classifyText(text);
    }

    public PolicyEligibilityBreadthClassification classify(PolicySearchProjection projection) {
        if (projection == null) {
            return new PolicyEligibilityBreadthClassification(EligibilityBreadth.UNKNOWN, List.of("대상·자격 정보 없음"));
        }
        String text = String.join(" ",
                nullToEmpty(projection.getTargetText()),
                nullToEmpty(projection.getQualificationText()),
                nullToEmpty(projection.getApplicationText()));
        return classifyText(text);
    }

    private PolicyEligibilityBreadthClassification classifyText(String text) {
        if (!StringUtils.hasText(text)) {
            return new PolicyEligibilityBreadthClassification(EligibilityBreadth.UNKNOWN, List.of("대상·자격 정보 부족"));
        }
        if (containsAny(text, "기초생활수급", "생계급여", "의료급여", "주거급여", "교육급여", "특정 사업 참여",
                "특정기관", "특정 기관", "특정학교", "특정 학교", "특정 업종", "임산부", "영유아", "수급 가구")) {
            return new PolicyEligibilityBreadthClassification(EligibilityBreadth.HIGHLY_RESTRICTED,
                    List.of("수급자·특정 기관/가구 구성원 등 고제한 자격"));
        }
        if (containsAny(text, "차상위", "중위소득", "저소득", "예술인", "농업인", "어업인", "소상공인", "창업자",
                "사업자", "다자녀", "한부모", "다문화", "특정 직종", "특정직종", "자격 보유", "자격증 보유")) {
            return new PolicyEligibilityBreadthClassification(EligibilityBreadth.RESTRICTED,
                    List.of("소득·직업·가구 형태 등 제한 자격"));
        }
        if (containsAny(text, "재직 청년", "재직자", "근로 청년", "미취업", "구직자", "대학생", "신혼부부",
                "19세", "20세", "24세", "29세", "34세")) {
            return new PolicyEligibilityBreadthClassification(EligibilityBreadth.MODERATE,
                    List.of("재직·미취업·학생·연령대 등 중간 범위 자격"));
        }
        if (containsAny(text, "청년 누구나", "지역 청년", "19~39세 청년", "19세~39세 청년", "대중교통 이용 청년",
                "청년", "일반 청년")) {
            return new PolicyEligibilityBreadthClassification(EligibilityBreadth.BROAD,
                    List.of("일반 청년 또는 지역 청년 대상"));
        }
        return new PolicyEligibilityBreadthClassification(EligibilityBreadth.UNKNOWN, List.of("범용성 판정 근거 부족"));
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
