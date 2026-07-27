package com.weaone.themoa.domain.policy.rag.service;

import com.weaone.themoa.domain.policy.policy.entity.Policy;
import com.weaone.themoa.domain.policy.policy.entity.PolicyGenderClassification;
import com.weaone.themoa.domain.policy.policy.entity.PolicyCategory;
import com.weaone.themoa.domain.policy.policy.entity.PolicySearchProjection;
import com.weaone.themoa.domain.policy.policy.repository.PolicyGenderClassificationRepository;
import com.weaone.themoa.domain.policy.policy.repository.PolicySearchProjectionRepository;
import com.weaone.themoa.domain.policy.rag.config.PolicyGenderClassificationProperties;
import com.weaone.themoa.domain.policy.rag.dto.PolicyApplicantScope;
import com.weaone.themoa.domain.policy.rag.dto.PolicyGenderAudience;
import com.weaone.themoa.domain.policy.rag.dto.PolicyGenderClassificationResult;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PolicyGenderAudienceClassifierTest {

    @Test
    void classifiesPersonalFemaleOnlyPolicyFromStructuredAiResult() {
        PolicyGenderAudienceClassifier classifier = classifier(new PolicyGenderAiAnalysis(
                "FEMALE_ONLY", true, 0.91, "INDIVIDUAL", "경력보유 여성 구직자 개인 대상"));

        PolicyGenderClassificationResult result = classifier.classifyProjection(projection(
                "경력보유여성 면접 정장 대여 지원",
                "경력단절 및 경력보유 여성 구직자",
                "신청일 기준 여성 구직자",
                "면접 정장 대여"));

        assertThat(result.audience()).isEqualTo(PolicyGenderAudience.FEMALE_ONLY);
        assertThat(result.exclusive()).isTrue();
        assertThat(result.applicantScope()).isEqualTo(PolicyApplicantScope.INDIVIDUAL);
    }

    @Test
    void doesNotTreatWomenOwnedBusinessPolicyAsFemaleOnlyPersonalPolicy() {
        PolicyGenderAudienceClassifier classifier = classifier(new PolicyGenderAiAnalysis(
                "UNKNOWN", false, 0.82, "BUSINESS", "여성기업 사업주 신청 정책"));

        PolicyGenderClassificationResult result = classifier.classifyProjection(projection(
                "여성기업 청년 채용 지원",
                "여성기업이 청년 근로자를 채용한 경우 사업주에게 지원",
                "사업주 신청",
                "청년 근로자 채용 지원"));

        assertThat(result.audience()).isNotEqualTo(PolicyGenderAudience.FEMALE_ONLY);
        assertThat(result.applicantScope()).isEqualTo(PolicyApplicantScope.BUSINESS);
    }

    @Test
    void invalidAiResponseFallsBackToUnknown() {
        PolicyGenderAudienceClassifier classifier = classifier(new PolicyGenderAiAnalysis(
                "WOMAN", true, 1.2, "GROUP", ""));

        PolicyGenderClassificationResult result = classifier.classifyProjection(projection(
                "여성가족부 청년 지원사업",
                "만 19~34세 청년 누구나",
                "청년 누구나",
                "지원"));

        assertThat(result.audience()).isEqualTo(PolicyGenderAudience.UNKNOWN);
        assertThat(result.exclusive()).isFalse();
    }

    @Test
    void skipsClassificationWhenVersionAndSourceHashAreSame() {
        PolicySearchProjection projection = projection("청년 문화 지원", "청년 누구나", "청년", "지원");
        PolicyGenderClassification classification = new PolicyGenderClassification(projection.getPolicy());
        String hash = new PolicyGenderSourceHasher().hash(projection);
        classification.update(new PolicyGenderClassificationResult(PolicyGenderAudience.ALL, false, 0.9,
                        PolicyApplicantScope.INDIVIDUAL, "성별 제한 없음"),
                PolicyGenderClassificationPolicy.VERSION, hash, projection.getUpdatedAt(), LocalDateTime.now());
        PolicySearchProjectionRepository projectionRepository = mock(PolicySearchProjectionRepository.class);
        PolicyGenderClassificationRepository classificationRepository = mock(PolicyGenderClassificationRepository.class);
        PolicyGenderAiClient aiClient = mock(PolicyGenderAiClient.class);
        when(projectionRepository.findAllActive()).thenReturn(List.of(projection));
        when(classificationRepository.findAll()).thenReturn(List.of(classification));
        PolicyGenderAudienceClassifier classifier = new PolicyGenderAudienceClassifier(projectionRepository,
                classificationRepository, aiClient, new PolicyGenderClassificationPolicy(), new PolicyGenderSourceHasher());

        PolicyGenderAudienceClassifier.GenderClassificationRebuildResult result = classifier.classifyMissingOrStale();

        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.processed()).isZero();
        verify(aiClient, org.mockito.Mockito.never()).analyze(any());
    }

    @Test
    void retriesAfterFailureAndStoresSuccessfulResult() {
        PolicySearchProjection projection = projection("청년 남성 교육", "청년 남성 개인", "남성 청년", "교육");
        PolicySearchProjectionRepository projectionRepository = mock(PolicySearchProjectionRepository.class);
        PolicyGenderClassificationRepository classificationRepository = mock(PolicyGenderClassificationRepository.class);
        PolicyGenderAiClient aiClient = mock(PolicyGenderAiClient.class);
        when(projectionRepository.findAllActive()).thenReturn(List.of(projection));
        when(classificationRepository.findAll()).thenReturn(List.of());
        AtomicInteger calls = new AtomicInteger();
        when(aiClient.analyze(any())).thenAnswer(invocation -> {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("429");
            }
            return new PolicyGenderAiAnalysis("MALE_ONLY", true, 0.92, "INDIVIDUAL", "남성 청년 개인 대상");
        });
        PolicyGenderAudienceClassifier classifier = new PolicyGenderAudienceClassifier(projectionRepository,
                classificationRepository, aiClient, new PolicyGenderClassificationPolicy(), new PolicyGenderSourceHasher(),
                fastProperties());

        PolicyGenderAudienceClassifier.GenderClassificationRebuildResult result = classifier.classifyMissingOrStale();

        assertThat(result.processed()).isEqualTo(1);
        assertThat(result.success()).isEqualTo(1);
        assertThat(result.retries()).isEqualTo(1);
        verify(classificationRepository).saveAll(org.mockito.ArgumentMatchers.argThat(items -> items.iterator().hasNext()));
    }

    @Test
    void apiTimeoutExceptionIsSavedAsUnknownFailureWithoutHangingJob() {
        PolicySearchProjection projection = projection("성별 모호 정책", "대상 불명확", "조건 불명확", "지원");
        PolicySearchProjectionRepository projectionRepository = mock(PolicySearchProjectionRepository.class);
        PolicyGenderClassificationRepository classificationRepository = mock(PolicyGenderClassificationRepository.class);
        PolicyGenderAiClient aiClient = mock(PolicyGenderAiClient.class);
        when(projectionRepository.findAllActive()).thenReturn(List.of(projection));
        when(classificationRepository.findAll()).thenReturn(List.of());
        when(aiClient.analyze(any())).thenThrow(new IllegalStateException("timeout"));
        PolicyGenderClassificationProperties properties = fastProperties();
        properties.setMaxRetries(1);
        PolicyGenderAudienceClassifier classifier = new PolicyGenderAudienceClassifier(projectionRepository,
                classificationRepository, aiClient, new PolicyGenderClassificationPolicy(), new PolicyGenderSourceHasher(),
                properties);

        PolicyGenderAudienceClassifier.GenderClassificationRebuildResult result = classifier.classifyMissingOrStale();

        assertThat(result.processed()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.unknown()).isEqualTo(1);
    }

    private PolicyGenderAudienceClassifier classifier(PolicyGenderAiAnalysis analysis) {
        PolicySearchProjectionRepository projectionRepository = mock(PolicySearchProjectionRepository.class);
        PolicyGenderClassificationRepository classificationRepository = mock(PolicyGenderClassificationRepository.class);
        PolicyGenderAiClient aiClient = mock(PolicyGenderAiClient.class);
        when(aiClient.analyze(org.mockito.ArgumentMatchers.any())).thenReturn(analysis);
        return new PolicyGenderAudienceClassifier(projectionRepository, classificationRepository, aiClient,
                new PolicyGenderClassificationPolicy(), new PolicyGenderSourceHasher());
    }

    private PolicyGenderClassificationProperties fastProperties() {
        PolicyGenderClassificationProperties properties = new PolicyGenderClassificationProperties();
        properties.setBatchSize(5);
        properties.setConcurrency(2);
        properties.setMaxRetries(2);
        properties.setRetryBackoff(Duration.ofMillis(1));
        properties.setCallTimeout(Duration.ofSeconds(1));
        return properties;
    }

    private PolicySearchProjection projection(String title, String target, String qualification, String support) {
        Policy policy = new Policy("P-GENDER");
        ReflectionTestUtils.setField(policy, "id", 1);
        policy.updateBasic(title, "기관", PolicyCategory.복지, title, null, null, null, true, true, "OPEN");
        PolicySearchProjection projection = new PolicySearchProjection(policy);
        projection.update(title, title, "", "", title, support, target, qualification,
                "", "기관", String.join(" ", List.of(title, target, qualification, support)), "test", false);
        return projection;
    }
}
