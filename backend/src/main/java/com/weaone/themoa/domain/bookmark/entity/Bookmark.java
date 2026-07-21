package com.weaone.themoa.domain.bookmark.entity;

import com.weaone.themoa.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 회원이 저장해 둔 관심 대상(금융상품 등). 마이페이지에서 한 번에 모아 본다.
 *
 * <p>대상은 {@link BookmarkTargetType} + target_id 조합으로 가리킨다. 한 컬럼이 여러 테이블을 참조할 수는
 * 없어서 target_id에는 FK를 걸지 않는다(다형 참조의 표준 트레이드오프). 상품 데이터는 finlife 배치가
 * upsert만 하고 행을 지우지는 않아 대상이 사라질 위험은 낮지만, 목록 조회 시 대상을 못 찾으면 건너뛴다.
 *
 * <p>같은 대상을 중복 저장하지 못하도록 (member_id, target_type, target_id) UNIQUE로 최종 보장한다.
 */
@Entity
@Table(
        name = "bookmark",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_bookmark_member_target",
                columnNames = {"member_id", "target_type", "target_id"}),
        indexes = @Index(name = "idx_bookmark_member_created", columnList = "member_id, created_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private BookmarkTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Bookmark(Member member, BookmarkTargetType targetType, Long targetId) {
        this.member = member;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public static Bookmark of(Member member, BookmarkTargetType targetType, Long targetId) {
        return new Bookmark(member, targetType, targetId);
    }
}
