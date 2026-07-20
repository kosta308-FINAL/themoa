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
 * 대출 금리/상환 옵션 (loan_product_option).
 * finlife 대출 API의 optionList 1건에 대응한다.
 * 대출 종류에 따라 채워지는 컬럼이 다르다(주담대: 담보/상환유형, 신용대출: 신용등급 구간 등).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "loan_product_option")
public class LoanProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private LoanProduct product;

    @Column(name = "mortgage_type_code")
    private String mortgageTypeCode;   // finlife: mrtg_type (담보유형코드, 주담대)

    @Column(name = "mortgage_type_name")
    private String mortgageTypeName;   // finlife: mrtg_type_nm

    @Column(name = "repay_type_code")
    private String repayTypeCode;      // finlife: rpay_type (상환유형코드)

    @Column(name = "repay_type_name")
    private String repayTypeName;      // finlife: rpay_type_nm

    @Column(name = "rate_type_code")
    private String rateTypeCode;       // finlife: lend_rate_type (금리유형코드)

    @Column(name = "rate_type_name")
    private String rateTypeName;       // finlife: lend_rate_type_nm

    @Column(name = "rate_min", precision = 5, scale = 2)
    private BigDecimal rateMin;        // finlife: lend_rate_min (최저금리)

    @Column(name = "rate_max", precision = 5, scale = 2)
    private BigDecimal rateMax;        // finlife: lend_rate_max (최고금리)

    @Column(name = "rate_avg", precision = 5, scale = 2)
    private BigDecimal rateAvg;        // finlife: lend_rate_avg (전월 평균금리)

    @Column(name = "credit_grade_section")
    private String creditGradeSection; // 신용등급 구간(신용대출 옵션 구분용)

    @Builder
    private LoanProductOption(String mortgageTypeCode, String mortgageTypeName, String repayTypeCode,
                              String repayTypeName, String rateTypeCode, String rateTypeName,
                              BigDecimal rateMin, BigDecimal rateMax, BigDecimal rateAvg,
                              String creditGradeSection) {
        this.mortgageTypeCode = mortgageTypeCode;
        this.mortgageTypeName = mortgageTypeName;
        this.repayTypeCode = repayTypeCode;
        this.repayTypeName = repayTypeName;
        this.rateTypeCode = rateTypeCode;
        this.rateTypeName = rateTypeName;
        this.rateMin = rateMin;
        this.rateMax = rateMax;
        this.rateAvg = rateAvg;
        this.creditGradeSection = creditGradeSection;
    }

    /** 연관관계 설정 - LoanProduct.addOption() 내부에서만 호출(패키지 전용). */
    void setProduct(LoanProduct product) {
        this.product = product;
    }
}
