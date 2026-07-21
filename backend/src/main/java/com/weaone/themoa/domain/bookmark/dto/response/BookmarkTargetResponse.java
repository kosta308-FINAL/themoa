package com.weaone.themoa.domain.bookmark.dto.response;

import com.weaone.themoa.domain.bookmark.entity.BookmarkTargetType;

/**
 * 회원이 북마크해 둔 대상 식별자만 담은 가벼운 응답.
 * 검색·추천 결과 목록에서 각 상품의 별표를 채울지(이미 북마크됨) 비울지 판단하는 데 쓴다.
 * 상세 정보가 필요 없으므로 상품 테이블을 조회하지 않는다.
 */
public record BookmarkTargetResponse(
        BookmarkTargetType targetType,
        Long targetId
) {
}
