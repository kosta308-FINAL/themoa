package com.weaone.themoa.domain.recommend.entity;

/**
 * 예·적금 상품 구분.
 * savings_product.product_type 컬럼에 Enum 이름(DEPOSIT/SAVING) 문자열로 저장된다.
 */
public enum SavingsType {
    DEPOSIT, // 정기예금
    SAVING   // 적금
}
