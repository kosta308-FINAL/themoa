package com.weaone.themoa.domain.bookmark.service;

import com.weaone.themoa.domain.bookmark.entity.BookmarkTargetType;

import java.util.Collection;
import java.util.Map;

/**
 * 북마크 대상 종류별 상세 조회 담당(확장 포인트).
 *
 * <p>정책 북마크를 추가하려면 {@link BookmarkTargetType}에 값을 하나 넣고 이 인터페이스 구현체를 하나 만들면 된다.
 * 스프링이 구현체를 모아 서비스에 주입하므로 {@code BookmarkService}·컨트롤러는 수정할 필요가 없다.
 *
 * <p>목록 화면에서 북마크 개수만큼 쿼리가 나가지 않도록, 한 건씩이 아니라 id 묶음을 받아 한 번에 조회한다.
 */
public interface BookmarkTargetReader {

    /** 이 리더가 처리하는 대상 종류. */
    BookmarkTargetType supportedType();

    /** targetId → 표시용 상세. 대상이 존재하지 않는 id는 결과에서 빠진다(호출측이 해당 북마크를 건너뛴다). */
    Map<Long, BookmarkTargetDetail> readAll(Collection<Long> targetIds);
}
