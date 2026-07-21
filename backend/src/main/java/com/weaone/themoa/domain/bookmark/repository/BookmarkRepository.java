package com.weaone.themoa.domain.bookmark.repository;

import com.weaone.themoa.domain.bookmark.entity.Bookmark;
import com.weaone.themoa.domain.bookmark.entity.BookmarkTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    /** 마이페이지 목록: 최근에 저장한 것부터. */
    List<Bookmark> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    boolean existsByMemberIdAndTargetTypeAndTargetId(Long memberId, BookmarkTargetType targetType, Long targetId);

    /** 해제. 없는 대상을 지워도 0건 삭제로 끝나 예외가 되지 않는다(토글 UX에서 중복 요청 대비). */
    long deleteByMemberIdAndTargetTypeAndTargetId(Long memberId, BookmarkTargetType targetType, Long targetId);
}
