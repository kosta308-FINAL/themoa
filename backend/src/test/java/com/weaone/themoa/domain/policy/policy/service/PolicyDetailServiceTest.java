package com.weaone.themoa.domain.policy.policy.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.policy.policy.domain.Policy;
import com.weaone.themoa.domain.policy.policy.domain.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.domain.PolicyRegion;
import com.weaone.themoa.domain.policy.policy.domain.RegionCode;
import com.weaone.themoa.domain.policy.policy.dto.response.PolicyDetailResponse;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class PolicyDetailServiceTest {
    private final PolicyRepository policyRepository = mock(PolicyRepository.class);
    private final PolicyDetailService service = new PolicyDetailService(policyRepository);

    @Test
    @DisplayName("정책 상세 조회는 Entity를 기존 JSON 필드 계약의 record DTO로 변환한다")
    void detailReturnsResponseRecord() {
        Policy policy = policy(10, "YC-10", "청년 정책");
        RegionCode region = new RegionCode(null, "M:41110", "경기도", "수원시", "CITY");
        policy.getRegions().add(new PolicyRegion(policy, region));
        given(policyRepository.findWithRelationsByIdIn(List.of(10))).willReturn(List.of(policy));

        PolicyDetailResponse response = service.detail(10);

        assertThat(response.policyId()).isEqualTo(10);
        assertThat(response.sourcePolicyId()).isEqualTo("YC-10");
        assertThat(response.title()).isEqualTo("청년 정책");
        assertThat(response.category()).isEqualTo("일자리");
        assertThat(response.summary()).isEqualTo("요약");
        assertThat(response.officialUrl()).isEqualTo("https://example.test/policy");
        assertThat(response.regions()).containsExactly("경기도 수원시");
    }

    @Test
    @DisplayName("정책이 없으면 기존 POLICY_NOT_FOUND 오류를 사용한다")
    void detailThrowsPolicyNotFound() {
        given(policyRepository.findWithRelationsByIdIn(List.of(404))).willReturn(List.of());

        assertThatThrownBy(() -> service.detail(404))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POLICY_NOT_FOUND);
    }

    private Policy policy(Integer id, String sourcePolicyId, String title) {
        Policy policy = new Policy(sourcePolicyId);
        ReflectionTestUtils.setField(policy, "id", id);
        policy.updateBasic(title, "청년기관", PolicyCategory.일자리, "요약", "https://example.test/policy",
                LocalDate.now(), LocalDate.now().plusDays(10), false, true, "OPEN");
        return policy;
    }
}
