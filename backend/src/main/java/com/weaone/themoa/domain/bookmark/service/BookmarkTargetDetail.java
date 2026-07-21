package com.weaone.themoa.domain.bookmark.service;

import java.math.BigDecimal;

/** 북마크 대상의 표시용 정보. 대상 종류별 리더가 채워서 돌려주고, 서비스가 응답 DTO로 조립한다. */
public record BookmarkTargetDetail(
        String title,
        String subtitle,
        BigDecimal rate,
        Integer termMonth
) {
}
