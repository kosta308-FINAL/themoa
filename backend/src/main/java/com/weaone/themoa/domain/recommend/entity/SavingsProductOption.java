package com.weaone.themoa.domain.recommend.entity;

import java.math.BigDecimal;

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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 예·적금 금리 옵션 (savings_product_option).
 * finlife 예금/적금 API의 optionList 1건(저축기간·금리유형 조합)에 대응한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "savings_product_option")
public class SavingsProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소유 상품 (FK: product_id) - 조회 시 필요할 때만 로딩되도록 지연 로딩 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private SavingsProduct product;

    @Column(name = "rate_type_code")
    private String rateTypeCode;       // finlife: intr_rate_type (S단리 / M복리)

    @Column(name = "rate_type_name")
    private String rateTypeName;       // finlife: intr_rate_type_nm

    @Column(name = "term_month")
    private Integer termMonth;         // finlife: save_trm (저축기간, 개월)

    @Column(name = "base_rate", precision = 5, scale = 2)
    private BigDecimal baseRate;       // finlife: intr_rate (기본(저축)금리)

    @Column(name = "max_rate", precision = 5, scale = 2)
    private BigDecimal maxRate;        // finlife: intr_rate2 (최고우대금리)

    @Column(name = "reserve_type_code")
    private String reserveTypeCode;    // finlife: rsrv_type (적립유형, 적금 전용)

    @Column(name = "reserve_type_name")
    private String reserveTypeName;    // finlife: rsrv_type_nm

    @Builder
    private SavingsProductOption(String rateTypeCode, String rateTypeName, Integer termMonth,
                                 BigDecimal baseRate, BigDecimal maxRate,
                                 String reserveTypeCode, String reserveTypeName) {
        this.rateTypeCode = rateTypeCode;
        this.rateTypeName = rateTypeName;
        this.termMonth = termMonth;
        this.baseRate = baseRate;
        this.maxRate = maxRate;
        this.reserveTypeCode = reserveTypeCode;
        this.reserveTypeName = reserveTypeName;
    }

    /** 연관관계 설정 - SavingsProduct.addOption() 내부에서만 호출된다(패키지 전용). */
    void setProduct(SavingsProduct product) {
        this.product = product;
    }
}
