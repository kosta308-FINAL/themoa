package com.weaone.themoa.domain.recommend.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 대출 상품 (loan_product).
 * finlife 주택담보/전세자금/개인신용 대출 API의 baseInfo 1건에 대응한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "loan_product")
public class LoanProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 대출 종류 구분 (Enum 이름을 문자열로 저장) */
    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 20)
    private LoanType productType;

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

    @Column(name = "loan_limit")
    private String loanLimit;          // finlife: loan_lmt (대출한도, 텍스트)

    @Column(name = "early_repay_fee")
    private String earlyRepayFee;      // finlife: erly_rpay_fee (중도상환수수료, 텍스트)

    @Column(name = "special_condition")
    private String specialCondition;   // 우대조건(테이블 컬럼 기준)

    @Column(name = "maturity_interest")
    private String maturityInterest;   // 만기 후 이자(테이블 컬럼 기준)

    @Column(name = "delay_rate")
    private String delayRate;          // finlife: dly_rate (연체이자율, 텍스트)

    @Column(name = "note")
    private String note;               // finlife: etc_note (기타 유의사항)

    @Column(name = "max_amount")
    private Integer maxAmount;         // 최고한도(숫자화 가능 시), 단위:만원

    @Column(name = "difficulty_tag")
    private String difficultyTag;      // 자동 태깅 결과

    @Column(name = "open_date", length = 8)
    private String openDate;           // finlife: dcls_strt_day (YYYYMMDD)

    @Column(name = "close_date", length = 8)
    private String closeDate;          // finlife: dcls_end_day - 값 있으면 판매종료

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 대출 금리/상환 옵션 목록. 상품과 함께 저장/삭제된다. */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LoanProductOption> options = new ArrayList<>();

    @Builder
    private LoanProduct(LoanType productType, String companyCode, String productCode,
                        String companyName, String productName, String joinMethod, String loanLimit,
                        String earlyRepayFee, String specialCondition, String maturityInterest,
                        String delayRate, String note, Integer maxAmount, String openDate, String closeDate) {
        this.productType = productType;
        this.companyCode = companyCode;
        this.productCode = productCode;
        this.companyName = companyName;
        this.productName = productName;
        this.joinMethod = joinMethod;
        this.loanLimit = loanLimit;
        this.earlyRepayFee = earlyRepayFee;
        this.specialCondition = specialCondition;
        this.maturityInterest = maturityInterest;
        this.delayRate = delayRate;
        this.note = note;
        this.maxAmount = maxAmount;
        this.openDate = openDate;
        this.closeDate = closeDate;
    }

    /** 연관관계 편의 메서드. */
    public void addOption(LoanProductOption option) {
        this.options.add(option);
        option.setProduct(this);
    }

    /** 자동 태깅 결과를 반영한다. */
    public void applyDifficultyTag(String difficultyTag) {
        this.difficultyTag = difficultyTag;
    }

    /** 배치 Upsert 갱신용 - finlife에서 새로 받은 기본정보를 기존 상품에 덮어쓴다. */
    public void updateBasicInfo(String companyName, String productName, String joinMethod,
                                String loanLimit, String earlyRepayFee, String specialCondition,
                                String maturityInterest, String delayRate, String note,
                                Integer maxAmount, String openDate, String closeDate) {
        this.companyName = companyName;
        this.productName = productName;
        this.joinMethod = joinMethod;
        this.loanLimit = loanLimit;
        this.earlyRepayFee = earlyRepayFee;
        this.specialCondition = specialCondition;
        this.maturityInterest = maturityInterest;
        this.delayRate = delayRate;
        this.note = note;
        this.maxAmount = maxAmount;
        this.openDate = openDate;
        this.closeDate = closeDate;
    }

    /** 옵션 전체 교체 - 기존 옵션은 orphanRemoval로 삭제되고 새 옵션으로 갈아끼운다. */
    public void replaceOptions(List<LoanProductOption> newOptions) {
        this.options.clear();
        newOptions.forEach(this::addOption);
    }
}
