package com.weaone.themoa.domain.policy.policy.entity;

import com.weaone.themoa.domain.policy.rag.dto.PolicyApplicantScope;
import com.weaone.themoa.domain.policy.rag.dto.PolicyGenderAudience;
import com.weaone.themoa.domain.policy.rag.dto.PolicyGenderClassificationResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

@Entity
@Table(name = "policy_gender_classification")
public class PolicyGenderClassification implements Persistable<Integer> {
    @Id
    @Column(name = "policy_id")
    private Integer policyId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "policy_id")
    private Policy policy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PolicyGenderAudience audience;

    @Column(nullable = false)
    private boolean exclusive;

    @Column(nullable = false)
    private double confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "applicant_scope", nullable = false, length = 30)
    private PolicyApplicantScope applicantScope;

    @Column(nullable = false, length = 500)
    private String evidence;

    @Column(name = "classification_version", nullable = false, length = 50)
    private String classificationVersion;

    @Column(name = "source_hash", nullable = false, length = 64)
    private String sourceHash;

    @Column(name = "source_updated_at")
    private LocalDateTime sourceUpdatedAt;

    @Column(name = "classified_at", nullable = false)
    private LocalDateTime classifiedAt;

    @Transient
    private boolean isNew = true;

    protected PolicyGenderClassification() {
    }

    public PolicyGenderClassification(Policy policy) {
        this.policy = policy;
        this.policyId = policy.getId();
    }

    public void update(PolicyGenderClassificationResult result, String classificationVersion,
                       String sourceHash, LocalDateTime sourceUpdatedAt, LocalDateTime classifiedAt) {
        this.audience = result.audience();
        this.exclusive = result.exclusive();
        this.confidence = result.confidence();
        this.applicantScope = result.applicantScope();
        this.evidence = trim(result.evidence());
        this.classificationVersion = classificationVersion;
        this.sourceHash = sourceHash;
        this.sourceUpdatedAt = sourceUpdatedAt;
        this.classifiedAt = classifiedAt;
    }

    public PolicyGenderClassificationResult toResult() {
        return new PolicyGenderClassificationResult(audience, exclusive, confidence, applicantScope, evidence);
    }

    public boolean stale(String currentVersion, String currentSourceHash) {
        return !classificationVersion.equals(currentVersion) || !sourceHash.equals(currentSourceHash);
    }

    @Override
    public Integer getId() {
        return policyId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    private void markNotNew() {
        this.isNew = false;
    }

    private String trim(String value) {
        if (value == null || value.isBlank()) {
            return "성별 조건을 확인할 수 없습니다.";
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    public Integer getPolicyId() {
        return policyId;
    }

    public PolicyGenderAudience getAudience() {
        return audience;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public double getConfidence() {
        return confidence;
    }

    public PolicyApplicantScope getApplicantScope() {
        return applicantScope;
    }

    public String getEvidence() {
        return evidence;
    }

    public String getClassificationVersion() {
        return classificationVersion;
    }

    public String getSourceHash() {
        return sourceHash;
    }
}
