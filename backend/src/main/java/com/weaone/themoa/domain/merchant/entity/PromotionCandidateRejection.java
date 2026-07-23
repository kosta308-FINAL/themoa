package com.weaone.themoa.domain.merchant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 관리자가 전역 마스터 승격 대기목록에서 특정 (표기, 제안 서비스명)을 "이건 틀렸다"고 판단해 반려한 기록.
 * 학습한 회원의 개인 표기(merchant_alias_terms)는 그대로 둔다 — 반려는 관리자의 전역 후보 큐에서만
 * 다시 안 보이게 할 뿐, 그 회원이 자기 화면에서 뭘 보든 관여하지 않는다(개인 학습은 항상 전역보다 우선).
 */
@Entity
@Table(name = "promotion_candidate_rejection",
        uniqueConstraints = @UniqueConstraint(columnNames = {"merchant_alias_id", "alias_text"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromotionCandidateRejection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_alias_id", nullable = false)
    private MerchantAlias merchantAlias;

    @Column(name = "alias_text", nullable = false, length = 255)
    private String aliasText;

    @Column(name = "rejected_at", nullable = false)
    private LocalDateTime rejectedAt;

    private PromotionCandidateRejection(MerchantAlias merchantAlias, String aliasText, LocalDateTime rejectedAt) {
        this.merchantAlias = merchantAlias;
        this.aliasText = aliasText;
        this.rejectedAt = rejectedAt;
    }

    public static PromotionCandidateRejection reject(MerchantAlias merchantAlias, String aliasText,
                                                       LocalDateTime rejectedAt) {
        return new PromotionCandidateRejection(merchantAlias, aliasText, rejectedAt);
    }
}
