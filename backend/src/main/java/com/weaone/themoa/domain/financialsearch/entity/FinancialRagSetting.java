package com.weaone.themoa.domain.financialsearch.entity;

import com.weaone.themoa.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 금융상품 검색 튜닝값(관리자가 화면에서 조정). 행이 없으면 application.yaml의 기본값을 쓴다.
 *
 * <p>한 행만 유지한다(고객센터 RAG 설정과 같은 방식). 검색 품질을 바꾸는 값이라 누가 언제 바꿨는지
 * 함께 남긴다.
 *
 * <p>벡터검색 ON/OFF와 컬렉션 이름은 여기서 다루지 않는다 — 벡터스토어 빈이 기동 시점에 만들어져서
 * 런타임에 바꿔도 실제로 반영되지 않기 때문이다(설정/재기동으로 다룬다).
 */
@Entity
@Table(name = "financial_rag_setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FinancialRagSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 최종 결과로 남길 상품 수. */
    @Column(name = "top_k", nullable = false)
    private int topK;

    /** 벡터검색에서 한 번에 받아올 후보 수(임계값 적용 전). */
    @Column(name = "retry_top_k", nullable = false)
    private int retryTopK;

    /** 코사인 유사도 최소값(0~1). 낮출수록 의미가 느슨하게 걸리는 상품까지 후보가 된다. */
    @Column(name = "minimum_similarity", nullable = false)
    private double minimumSimilarity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Member updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private FinancialRagSetting(int topK, int retryTopK, double minimumSimilarity,
                                Member updatedBy, LocalDateTime now) {
        this.topK = topK;
        this.retryTopK = retryTopK;
        this.minimumSimilarity = minimumSimilarity;
        this.updatedBy = updatedBy;
        this.updatedAt = now;
    }

    public static FinancialRagSetting create(int topK, int retryTopK, double minimumSimilarity,
                                             Member updatedBy, LocalDateTime now) {
        return new FinancialRagSetting(topK, retryTopK, minimumSimilarity, updatedBy, now);
    }

    public void update(int topK, int retryTopK, double minimumSimilarity, Member updatedBy, LocalDateTime now) {
        this.topK = topK;
        this.retryTopK = retryTopK;
        this.minimumSimilarity = minimumSimilarity;
        this.updatedBy = updatedBy;
        this.updatedAt = now;
    }
}
