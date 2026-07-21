package com.weaone.themoa.domain.bookmark.dto.response;

import com.weaone.themoa.domain.bookmark.entity.BookmarkTargetType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 마이페이지 북마크 목록 1건.
 *
 * <p>대상 종류가 늘어나도(정책 등) 같은 형태로 보여줄 수 있도록 제목/부제목 중심의 공통 필드로 둔다.
 *
 * @param title      상품명(정책이면 정책명)
 * @param subtitle   금융회사명(정책이면 주관기관)
 * @param rate       대표금리. 예·적금은 최고금리, 대출은 최저금리. 금리 개념이 없는 대상이면 null
 * @param termMonth  대표 가입기간(개월). 대출처럼 기간 개념이 없으면 null
 */
public record BookmarkResponse(
        Long bookmarkId,
        BookmarkTargetType targetType,
        Long targetId,
        String title,
        String subtitle,
        BigDecimal rate,
        Integer termMonth,
        LocalDateTime createdAt
) {
}
