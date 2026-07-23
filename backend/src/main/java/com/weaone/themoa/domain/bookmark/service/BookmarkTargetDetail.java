package com.weaone.themoa.domain.bookmark.service;

import java.math.BigDecimal;

/**
 * 북마크 대상의 표시용 정보. 대상 종류별 리더가 채워서 돌려주고, 서비스가 응답 DTO로 조립한다.
 *
 * <p>마이페이지에서도 검색 결과와 같은 수준으로 보여주기 위해 금리뿐 아니라 가입방법·우대조건·공식 링크까지 담는다.
 */
public record BookmarkTargetDetail(
        String title,
        String subtitle,
        String productType,
        String joinMethod,
        BigDecimal rate,
        Integer termMonth,
        String specialCondition,
        String officialUrl,
        boolean discontinued
) {
}
