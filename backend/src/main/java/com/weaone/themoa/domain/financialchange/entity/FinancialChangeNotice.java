package com.weaone.themoa.domain.financialchange.entity;

import com.weaone.themoa.domain.bookmark.entity.BookmarkTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 관심 상품에 변경이 감지된 사건 1건. 알림을 눌렀을 때 보여줄 "이전 → 이후" 내용을 담는다.
 *
 * <p>변경이 생길 때마다 새 행을 쌓는다(합치지 않는다). 5%→4%→3%로 두 번 바뀌었다면 행도 알림도 2건이고,
 * 각각 그 시점의 변화를 정확히 보여준다.
 *
 * <p>알림 테이블에 우리 쪽 FK를 추가하지 않기 위해, 알림과는 {@code dedupKey} 문자열로 연결한다
 * (알림 생성 시 쓴 것과 같은 값).
 */
@Entity
@Table(
        name = "financial_change_notice",
        indexes = {
                @Index(name = "idx_financial_change_notice_member", columnList = "member_id, created_at"),
                @Index(name = "idx_financial_change_notice_dedup", columnList = "member_id, dedup_key")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FinancialChangeNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private BookmarkTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /** 알림 목록에서 다시 조회하지 않아도 되도록 상품명·회사명을 함께 남긴다(상품이 사라져도 표시 가능). */
    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "company_name", length = 100)
    private String companyName;

    @Column(name = "previous_rate", precision = 5, scale = 2)
    private BigDecimal previousRate;

    @Column(name = "current_rate", precision = 5, scale = 2)
    private BigDecimal currentRate;

    @Column(name = "previous_special_condition", columnDefinition = "TEXT")
    private String previousSpecialCondition;

    @Column(name = "current_special_condition", columnDefinition = "TEXT")
    private String currentSpecialCondition;

    /** 이번 변경으로 판매종료가 되었는지. */
    @Column(name = "discontinued", nullable = false)
    private boolean discontinued;

    /** 알림과 연결하는 키(알림 생성 시 쓴 dedupKey와 같은 값). */
    @Column(name = "dedup_key", nullable = false, length = 150)
    private String dedupKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private FinancialChangeNotice(Long memberId, BookmarkTargetType targetType, Long targetId,
                                  String productName, String companyName,
                                  BigDecimal previousRate, BigDecimal currentRate,
                                  String previousSpecialCondition, String currentSpecialCondition,
                                  boolean discontinued, String dedupKey, LocalDateTime now) {
        this.memberId = memberId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.productName = productName;
        this.companyName = companyName;
        this.previousRate = previousRate;
        this.currentRate = currentRate;
        this.previousSpecialCondition = previousSpecialCondition;
        this.currentSpecialCondition = currentSpecialCondition;
        this.discontinued = discontinued;
        this.dedupKey = dedupKey;
        this.createdAt = now;
    }

    public static FinancialChangeNotice of(Long memberId, BookmarkTargetType targetType, Long targetId,
                                           String productName, String companyName,
                                           BigDecimal previousRate, BigDecimal currentRate,
                                           String previousSpecialCondition, String currentSpecialCondition,
                                           boolean discontinued, String dedupKey, LocalDateTime now) {
        return new FinancialChangeNotice(memberId, targetType, targetId, productName, companyName,
                previousRate, currentRate, previousSpecialCondition, currentSpecialCondition,
                discontinued, dedupKey, now);
    }
}
