package com.weaone.themoa.domain.recommend.entity;

import com.weaone.themoa.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 회원이 가장 최근에 받은 맞춤 추천 결과(top N).
 *
 * <p>변경 알림 대상을 정하는 데 쓴다 — 북마크한 상품뿐 아니라 "최근 추천받은 상품"의 금리가 바뀌어도
 * 알려주기 위함이다.
 *
 * <p>추천을 다시 받으면 이전 기록은 지우고 새 결과로 교체한다(최신 1회분만 유지). 비교 기준값은 여기가
 * 아니라 별도 스냅샷 테이블이 관리하므로, 여기에는 어떤 상품이 몇 위였는지만 담는다.
 */
@Entity
@Table(
        name = "recommend_snapshot",
        indexes = @Index(name = "idx_recommend_snapshot_member", columnList = "member_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 추천은 예·적금만 다루지만, 이후 대출 추천이 생겨도 같은 구조로 쓸 수 있게 유형을 함께 둔다. */
    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /** 추천 목록에서의 순위(1위부터). */
    @Column(name = "rank_no", nullable = false)
    private int rankNo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private RecommendSnapshot(Member member, String targetType, Long targetId, int rankNo, LocalDateTime now) {
        this.member = member;
        this.targetType = targetType;
        this.targetId = targetId;
        this.rankNo = rankNo;
        this.createdAt = now;
    }

    public static RecommendSnapshot of(Member member, String targetType, Long targetId, int rankNo,
                                       LocalDateTime now) {
        return new RecommendSnapshot(member, targetType, targetId, rankNo, now);
    }
}
