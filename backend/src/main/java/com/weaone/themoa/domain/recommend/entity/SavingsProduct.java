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
 * 예·적금 상품 (savings_product).
 * finlife 예금/적금 API의 baseInfo(상품 기본정보) 1건에 대응한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 전용 기본 생성자 (외부 무분별 생성 차단)
@Entity
@Table(name = "savings_product")
public class SavingsProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL AUTO_INCREMENT
    private Long id;

    /** 예금/적금 구분 (Enum 이름을 문자열로 저장) */
    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 20)
    private SavingsType productType;

    @Column(name = "company_code", nullable = false)
    private String companyCode;        // finlife: fin_co_no (금융회사코드) - 중복체크 키의 일부

    @Column(name = "product_code", nullable = false)
    private String productCode;        // finlife: fin_prdt_cd (상품코드) - 회사코드와 조합해야 유니크

    @Column(name = "company_name")
    private String companyName;        // finlife: kor_co_nm (금융회사명)

    @Column(name = "product_name")
    private String productName;        // finlife: fin_prdt_nm (상품명)

    @Column(name = "join_method")
    private String joinMethod;         // finlife: join_way (가입방법)

    @Column(name = "join_restrict")
    private String joinRestrict;       // finlife: join_deny (가입제한: 1제한없음 2서민전용 3일부제한)

    @Column(name = "join_target")
    private String joinTarget;         // finlife: join_member (가입대상) → 파싱 원본

    @Column(name = "special_condition")
    private String specialCondition;   // finlife: spcl_cnd (우대조건)

    @Column(name = "maturity_interest")
    private String maturityInterest;   // finlife: mtrt_int (만기 후 이자율)

    @Column(name = "note")
    private String note;               // finlife: etc_note (기타 유의사항)

    @Column(name = "max_amount")
    private Integer maxAmount;         // finlife: max_limit (최고한도) - INT, 단위:원

    @Column(name = "open_date", length = 8)
    private String openDate;           // finlife: dcls_strt_day (공시 시작일 YYYYMMDD)

    @Column(name = "close_date", length = 8)
    private String closeDate;          // finlife: dcls_end_day (공시 종료일) - 값 있으면 판매종료

    // ----- 자동 태깅 결과 -----
    @Column(name = "is_online")
    private Boolean isOnline;

    @Column(name = "difficulty_tag")
    private String difficultyTag;

    @Column(name = "is_youth_friendly")
    private Boolean isYouthFriendly;

    // ----- join_target 파싱 결과 -----
    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Column(name = "income_limit")
    private Integer incomeLimit;       // 소득 상한(만원)

    @Column(name = "income_min")
    private Integer incomeMin;         // 소득 하한(만원)

    @Column(name = "employment_type")
    private String employmentType;     // 재직형태(정규직/근로자 등)

    @Column(name = "is_for_low_income")
    private Boolean isForLowIncome;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;   // 저장/수정 시각 자동 반영

    /** 금리 옵션 목록. 상품 저장 시 함께 저장되고(cascade), 목록에서 빠지면 삭제된다(orphanRemoval). */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SavingsProductOption> options = new ArrayList<>();

    @Builder
    private SavingsProduct(SavingsType productType, String companyCode, String productCode,
                           String companyName, String productName, String joinMethod, String joinRestrict,
                           String joinTarget, String specialCondition, String maturityInterest,
                           String note, Integer maxAmount, String openDate, String closeDate) {
        this.productType = productType;
        this.companyCode = companyCode;
        this.productCode = productCode;
        this.companyName = companyName;
        this.productName = productName;
        this.joinMethod = joinMethod;
        this.joinRestrict = joinRestrict;
        this.joinTarget = joinTarget;
        this.specialCondition = specialCondition;
        this.maturityInterest = maturityInterest;
        this.note = note;
        this.maxAmount = maxAmount;
        this.openDate = openDate;
        this.closeDate = closeDate;
    }

    /** 연관관계 편의 메서드 - 양방향(부모↔자식) 참조를 함께 세팅한다. */
    public void addOption(SavingsProductOption option) {
        this.options.add(option);
        option.setProduct(this);
    }

    /** join_target 파싱 결과를 반영한다. */
    public void applyParsedCondition(Integer minAge, Integer maxAge, Integer incomeLimit,
                                     Integer incomeMin, String employmentType, Boolean isForLowIncome) {
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.incomeLimit = incomeLimit;
        this.incomeMin = incomeMin;
        this.employmentType = employmentType;
        this.isForLowIncome = isForLowIncome;
    }

    /** 자동 태깅 결과를 반영한다. */
    public void applyTags(Boolean isOnline, String difficultyTag, Boolean isYouthFriendly) {
        this.isOnline = isOnline;
        this.difficultyTag = difficultyTag;
        this.isYouthFriendly = isYouthFriendly;
    }

    /**
     * 배치 Upsert 갱신용 - finlife에서 새로 받은 기본정보를 기존 상품에 덮어쓴다.
     * (식별자인 productType/companyCode/productCode는 바뀌지 않으므로 제외)
     */
    public void updateBasicInfo(String companyName, String productName, String joinMethod,
                                String joinRestrict, String joinTarget, String specialCondition,
                                String maturityInterest, String note, Integer maxAmount,
                                String openDate, String closeDate) {
        this.companyName = companyName;
        this.productName = productName;
        this.joinMethod = joinMethod;
        this.joinRestrict = joinRestrict;
        this.joinTarget = joinTarget;
        this.specialCondition = specialCondition;
        this.maturityInterest = maturityInterest;
        this.note = note;
        this.maxAmount = maxAmount;
        this.openDate = openDate;
        this.closeDate = closeDate;
    }

    /** 옵션 전체 교체 - 기존 옵션은 orphanRemoval로 삭제되고 새 옵션으로 갈아끼운다. */
    public void replaceOptions(List<SavingsProductOption> newOptions) {
        this.options.clear();
        newOptions.forEach(this::addOption);
    }
}
