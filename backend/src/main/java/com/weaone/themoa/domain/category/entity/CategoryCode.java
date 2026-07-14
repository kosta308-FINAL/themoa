package com.weaone.themoa.domain.category.entity;

/**
 * 전역 카테고리 마스터(category.md §3)의 불변 비즈니스 키. {@link Category#getCode()}가 저장하는
 * 값과 1:1로 대응한다. 코드에서는 매직넘버 대신 이 enum으로 카테고리를 참조한다.
 */
public enum CategoryCode {
    FOOD,
    DELIVERY,
    CAFE,
    CONVENIENCE,
    TRANSPORT,
    SHOPPING,
    SUBSCRIPTION,
    LEISURE,
    MEDICAL,
    BEAUTY,
    DONATION,
    ETC
}
