package com.weaone.themoa.domain.customerservice.entity;

/** 1:1 문의 처리 상태(erd.md §8). MVP는 화면 분기가 있는 두 값만 사용한다(customerservice.md §0). */
public enum InquiryStatus {
    PENDING,
    ANSWERED
}
