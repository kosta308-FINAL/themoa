package com.weaone.themoa.domain.financialchange.entity;

import com.weaone.themoa.domain.bookmark.entity.BookmarkTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 회원이 마지막으로 "확인한 상태"의 상품 정보. 변경 감지의 비교 기준값이다.
 *
 * <p>감지 배치가 현재 상품 정보와 이 값을 비교해 다르면 알림을 만들고, 곧바로 이 값을 최신으로 갱신한다.
 * 그래서 다음 변경은 자연히 "직전 알림 이후의 변화"가 된다(5%→4% 알림 후 3%가 되면 4%→3%로 알림).
 *
 * <p>대상은 북마크한 상품과 최근 추천받은 상품 모두이며, 같은 상품이 양쪽에 있어도 기준값은 하나만 둔다
 * (알림이 두 번 가지 않도록).
 */
@Entity
@Table(
        name = "financial_watch_snapshot",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_financial_watch_snapshot",
                columnNames = {"member_id", "target_type", "target_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FinancialWatchSnapshot {

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

    /** 대표금리(예·적금은 최고금리, 대출은 최저금리). 옵션이 없으면 null일 수 있다. */
    @Column(name = "rate", precision = 5, scale = 2)
    private BigDecimal rate;

    @Column(name = "special_condition", columnDefinition = "TEXT")
    private String specialCondition;

    @Column(name = "discontinued", nullable = false)
    private boolean discontinued;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private FinancialWatchSnapshot(Long memberId, BookmarkTargetType targetType, Long targetId,
                                   BigDecimal rate, String specialCondition, boolean discontinued,
                                   LocalDateTime now) {
        this.memberId = memberId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.rate = rate;
        this.specialCondition = specialCondition;
        this.discontinued = discontinued;
        this.updatedAt = now;
    }

    public static FinancialWatchSnapshot of(Long memberId, BookmarkTargetType targetType, Long targetId,
                                            BigDecimal rate, String specialCondition, boolean discontinued,
                                            LocalDateTime now) {
        return new FinancialWatchSnapshot(memberId, targetType, targetId, rate, specialCondition,
                discontinued, now);
    }

    /** 알림을 만든 뒤 최신 상태로 갱신한다. */
    public void update(BigDecimal rate, String specialCondition, boolean discontinued, LocalDateTime now) {
        this.rate = rate;
        this.specialCondition = specialCondition;
        this.discontinued = discontinued;
        this.updatedAt = now;
    }
}
