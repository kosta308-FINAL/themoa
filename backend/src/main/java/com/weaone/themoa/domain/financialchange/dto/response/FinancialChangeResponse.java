package com.weaone.themoa.domain.financialchange.dto.response;

import com.weaone.themoa.domain.bookmark.entity.BookmarkTargetType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 관심 상품 변경 상세(알림을 눌렀을 때 띄우는 팝업 내용).
 *
 * @param previousRate             이전 금리. 화면에서 "3.9% → 3.5%"처럼 빨간색으로 강조한다
 * @param rateChanged              금리가 실제로 바뀌었는지(바뀐 항목만 강조하기 위해)
 * @param specialConditionChanged  우대조건이 바뀌었는지
 * @param discontinued             판매종료되었는지
 * @param history                  같은 상품의 이전 변동 이력(최근 것부터). 추이를 보고 싶을 때 쓴다
 */
public record FinancialChangeResponse(
        Long changeId,
        BookmarkTargetType targetType,
        Long targetId,
        String productName,
        String companyName,
        BigDecimal previousRate,
        BigDecimal currentRate,
        boolean rateChanged,
        String previousSpecialCondition,
        String currentSpecialCondition,
        boolean specialConditionChanged,
        boolean discontinued,
        LocalDateTime createdAt,
        List<HistoryItem> history
) {

    /** 변동 이력 1건(추이 표시용). */
    public record HistoryItem(
            BigDecimal previousRate,
            BigDecimal currentRate,
            boolean discontinued,
            LocalDateTime createdAt
    ) {
    }
}
