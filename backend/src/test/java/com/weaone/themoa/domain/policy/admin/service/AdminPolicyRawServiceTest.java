package com.weaone.themoa.domain.policy.admin.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.policy.admin.dto.response.AdminPolicyRawResponse;
import com.weaone.themoa.domain.policy.policy.domain.Policy;
import com.weaone.themoa.domain.policy.policy.domain.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.domain.PolicyRawData;
import com.weaone.themoa.domain.policy.policy.domain.PolicySourceSnapshot;
import com.weaone.themoa.domain.policy.policy.repository.PolicyRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicySourceSnapshotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class AdminPolicyRawServiceTest {
    private final PolicyRepository policyRepository = mock(PolicyRepository.class);
    private final PolicySourceSnapshotRepository snapshotRepository = mock(PolicySourceSnapshotRepository.class);
    private final AdminPolicyRawService service = new AdminPolicyRawService(policyRepository, snapshotRepository);

    @Test
    @DisplayName("관리자 raw 조회는 snapshot과 raw data를 관리 전용 DTO로 변환한다")
    void rawReturnsSnapshotResponse() {
        Policy policy = policy(10, "YC-10", "청년 정책");
        PolicyRawData rawData = new PolicyRawData("YOUTH_CENTER", "YC-10", "https://api.test",
                "{}", "{}", "JSON", "SUCCESS", null);
        PolicySourceSnapshot snapshot = new PolicySourceSnapshot(policy, rawData, "YOUTH_CENTER", "YC-10",
                "{\"id\":\"YC-10\"}", "hash");
        given(policyRepository.findById(10)).willReturn(Optional.of(policy));
        given(snapshotRepository.findByPolicyId(10)).willReturn(Optional.of(snapshot));

        AdminPolicyRawResponse response = service.raw(10);

        assertThat(response.policyId()).isEqualTo(10);
        assertThat(response.sourcePolicyId()).isEqualTo("YC-10");
        assertThat(response.source()).isEqualTo("YOUTH_CENTER");
        assertThat(response.rawPolicy()).isEqualTo("{\"id\":\"YC-10\"}");
        assertThat(response.pageRawData().requestUrl()).isEqualTo("https://api.test");
        assertThat(response.pageRawData().responseFormat()).isEqualTo("JSON");
        assertThat(response.pageRawData().collectedAt()).isNotBlank();
    }

    @Test
    @DisplayName("snapshot이 없으면 관리자 raw 조회도 POLICY_NOT_FOUND 오류를 사용한다")
    void rawThrowsPolicyNotFoundWhenSnapshotMissing() {
        Policy policy = policy(10, "YC-10", "청년 정책");
        given(policyRepository.findById(10)).willReturn(Optional.of(policy));
        given(snapshotRepository.findByPolicyId(10)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.raw(10))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POLICY_NOT_FOUND);
    }

    @Test
    @DisplayName("raw data가 없어도 pageRawData는 빈 record 객체로 유지한다")
    void rawReturnsEmptyPageRawDataWhenRawDataMissing() {
        Policy policy = policy(10, "YC-10", "청년 정책");
        PolicySourceSnapshot snapshot = new PolicySourceSnapshot(policy, null, "YOUTH_CENTER", "YC-10",
                "{\"id\":\"YC-10\"}", "hash");
        given(policyRepository.findById(10)).willReturn(Optional.of(policy));
        given(snapshotRepository.findByPolicyId(10)).willReturn(Optional.of(snapshot));

        AdminPolicyRawResponse response = service.raw(10);

        assertThat(response.pageRawData()).isNotNull();
        assertThat(response.pageRawData().rawDataId()).isNull();
    }

    private Policy policy(Integer id, String sourcePolicyId, String title) {
        Policy policy = new Policy(sourcePolicyId);
        ReflectionTestUtils.setField(policy, "id", id);
        policy.updateBasic(title, "청년기관", PolicyCategory.일자리, "요약", "https://example.test/policy",
                LocalDate.now(), LocalDate.now().plusDays(10), false, true, "OPEN");
        return policy;
    }
}
