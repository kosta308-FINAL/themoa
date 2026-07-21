/**
 * 상품 종류(productType)를 북마크 대상 타입(BookmarkTargetType)으로 변환한다.
 * 예·적금과 대출은 각각 다른 테이블이라 id만으로는 대상을 특정할 수 없어 타입이 함께 필요하다.
 */
const LOAN_PRODUCT_TYPES = new Set(["MORTGAGE", "RENT", "CREDIT"]);

export const SAVINGS_PRODUCT = "SAVINGS_PRODUCT";
export const LOAN_PRODUCT = "LOAN_PRODUCT";

export const bookmarkTargetTypeOf = (productType) =>
  LOAN_PRODUCT_TYPES.has(productType) ? LOAN_PRODUCT : SAVINGS_PRODUCT;
