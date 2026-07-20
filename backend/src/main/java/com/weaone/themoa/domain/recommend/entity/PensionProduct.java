package com.weaone.themoa.domain.recommend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 연금저축 상품 (pension_product).
 * finlife 연금저축 API의 baseInfo(상품 기본정보) 1건에 대응한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "pension_product")
public class PensionProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_code", nullable = false)
    private String companyCode;        // finlife: fin_co_no (금융회사코드) - 중복체크 키의 일부

    @Column(name = "product_code", nullable = false)
    private String productCode;        // finlife: fin_prdt_cd - 회사코드와 조합해야 유니크

    @Column(name = "company_name")
    private String companyName;        // finlife: kor_co_nm

    @Column(name = "product_name")
    private String productName;        // finlife: fin_prdt_nm

    @Column(name = "join_method")
    private String joinMethod;         // finlife: join_way

    @Column(name = "pension_kind_code")
    private String pensionKindCode;    // finlife: pnsn_kind (연금종류코드)

    @Column(name = "pension_kind_name")
    private String pensionKindName;    // finlife: pnsn_kind_nm

    @Column(name = "product_type_code")
    private String productTypeCode;    // finlife: prdt_type (상품유형코드)

    @Column(name = "product_type_name")
    private String productTypeName;    // finlife: prdt_type_nm

    @Column(name = "avg_profit_rate", precision = 6, scale = 2)
    private BigDecimal avgProfitRate;  // finlife: avg_prft_rate (평균 수익률)

    @Column(name = "profit_rate_1yr", precision = 6, scale = 2)
    private BigDecimal profitRate1yr;  // 직전 1년 수익률

    @Column(name = "profit_rate_2yr", precision = 6, scale = 2)
    private BigDecimal profitRate2yr;  // 직전 2년 수익률

    @Column(name = "profit_rate_3yr", precision = 6, scale = 2)
    private BigDecimal profitRate3yr;  // 직전 3년 수익률

    @Column(name = "sale_company")
    private String saleCompany;        // finlife: sale_co (판매회사)

    @Column(name = "open_date", length = 8)
    private String openDate;           // finlife: dcls_strt_day (YYYYMMDD)

    @Column(name = "close_date", length = 8)
    private String closeDate;          // finlife: dcls_end_day - 값 있으면 판매종료

    // ----- join 조건 파싱 결과 -----
    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Column(name = "income_limit")
    private Integer incomeLimit;       // 소득 상한(만원)

    @Column(name = "income_min")
    private Integer incomeMin;         // 소득 하한(만원)

    @Column(name = "employment_type")
    private String employmentType;

    @Column(name = "is_for_low_income")
    private Boolean isForLowIncome;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 연금 수령 옵션 목록. 상품과 함께 저장/삭제된다. */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PensionProductOption> options = new ArrayList<>();

    @Builder
    private PensionProduct(String companyCode, String productCode, String companyName, String productName,
                           String joinMethod, String pensionKindCode, String pensionKindName,
                           String productTypeCode, String productTypeName, BigDecimal avgProfitRate,
                           BigDecimal profitRate1yr, BigDecimal profitRate2yr, BigDecimal profitRate3yr,
                           String saleCompany, String openDate, String closeDate) {
        this.companyCode = companyCode;
        this.productCode = productCode;
        this.companyName = companyName;
        this.productName = productName;
        this.joinMethod = joinMethod;
        this.pensionKindCode = pensionKindCode;
        this.pensionKindName = pensionKindName;
        this.productTypeCode = productTypeCode;
        this.productTypeName = productTypeName;
        this.avgProfitRate = avgProfitRate;
        this.profitRate1yr = profitRate1yr;
        this.profitRate2yr = profitRate2yr;
        this.profitRate3yr = profitRate3yr;
        this.saleCompany = saleCompany;
        this.openDate = openDate;
        this.closeDate = closeDate;
    }

    /** 연관관계 편의 메서드. */
    public void addOption(PensionProductOption option) {
        this.options.add(option);
        option.setProduct(this);
    }

    /** 가입 조건 파싱 결과를 반영한다. */
    public void applyParsedCondition(Integer minAge, Integer maxAge, Integer incomeLimit,
                                     Integer incomeMin, String employmentType, Boolean isForLowIncome) {
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.incomeLimit = incomeLimit;
        this.incomeMin = incomeMin;
        this.employmentType = employmentType;
        this.isForLowIncome = isForLowIncome;
    }
}
