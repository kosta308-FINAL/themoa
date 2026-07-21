package com.weaone.themoa.domain.bookmark.entity;

/**
 * 북마크 대상의 종류.
 *
 * <p>금융상품은 예·적금(savings_product)과 대출(loan_product)이 서로 다른 테이블이고 PK도 각각 독립적이라,
 * target_id 하나만으로는 어떤 상품인지 특정할 수 없다. 그래서 "타입 + id" 조합으로 대상을 식별한다.
 *
 * <p>확장: 정책 북마크가 필요해지면 여기에 {@code POLICY}를 추가하고, 그 타입을 지원하는
 * {@code BookmarkTargetReader} 구현체 하나만 만들면 된다(서비스/컨트롤러 수정 불필요).
 */
public enum BookmarkTargetType {
    SAVINGS_PRODUCT, // 예금·적금 (savings_product)
    LOAN_PRODUCT     // 대출 (loan_product)
}
