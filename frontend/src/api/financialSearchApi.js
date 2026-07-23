import axiosInstance from "./axiosInstance";

const responseData = (response) => response.data?.data;

/**
 * 금융상품 검색. sort를 생략하면 백엔드가 RELEVANCE(관련도순)로 처리한다.
 * sort: RELEVANCE | RATE_DESC | RATE_ASC | TERM_ASC
 */
export const searchFinancialProducts = ({ query, sort }) =>
  axiosInstance
    .post("/api/financial-products/search", { query, sort })
    .then(responseData);

/**
 * 실시간 인기 금융상품 순위(북마크 많은 순).
 * data: [{ rank, productId, productName, companyName, productType, rate, termMonth, bookmarkCount, officialUrl }]
 */
export const getPopularProducts = (limit = 10) =>
  axiosInstance
    .get("/api/financial-products/popular", { params: { limit } })
    .then(responseData);
