package com.weaone.themoa.domain.recommend.entity;

/**
 * 대출 상품 구분.
 * loan_product.product_type 컬럼에 Enum 이름(MORTGAGE/RENT/CREDIT) 문자열로 저장된다.
 */
public enum LoanType {
    MORTGAGE, // 주택담보대출
    RENT,     // 전세자금대출
    CREDIT    // 개인신용대출
}
