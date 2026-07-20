package com.weaone.themoa.domain.cardtransaction.service;

/**
 * 카테고리 소비 상세(categoryDetail.md §4.2)의 {@code changeStatus}·{@code insights.direction} 공용 값.
 * {@code changeStatus}는 {@code MIXED}를 쓰지 않고, {@code insights.direction}만 {@code MIXED}를 쓴다.
 */
public enum ChangeDirection {
    INCREASED,
    DECREASED,
    UNCHANGED,
    NEW,
    MIXED
}
