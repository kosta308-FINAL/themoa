package com.weaone.themoa.domain.bookmark.repository;

import com.weaone.themoa.domain.bookmark.entity.Bookmark;
import com.weaone.themoa.domain.bookmark.entity.BookmarkTargetType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    /** 대상 종류별로 북마크가 많은 상품 순위(실시간 인기). targetId + 담은 사람 수. */
    @Query("""
            select b.targetId as targetId, count(b) as count
            from Bookmark b
            where b.targetType = :targetType
            group by b.targetId
            order by count(b) desc
            """)
    List<PopularTarget> findPopularTargets(@Param("targetType") BookmarkTargetType targetType, Pageable pageable);

    /** 인기 순위 집계 결과 투영. */
    interface PopularTarget {
        Long getTargetId();
        long getCount();
    }


    /** 마이페이지 목록: 최근에 저장한 것부터. */
    List<Bookmark> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    boolean existsByMemberIdAndTargetTypeAndTargetId(Long memberId, BookmarkTargetType targetType, Long targetId);

    /** 해제. 없는 대상을 지워도 0건 삭제로 끝나 예외가 되지 않는다(토글 UX에서 중복 요청 대비). */
    long deleteByMemberIdAndTargetTypeAndTargetId(Long memberId, BookmarkTargetType targetType, Long targetId);
}
