import axiosInstance from "./axiosInstance";

const responseData = (response) => response.data?.data;

/**
 * 맞춤 금융상품 추천 요청. payload는 백엔드 RecommendRequest 형태를 따른다.
 * (age, monthlyIncomeManwon, employmentType, lowIncome, riskType, preferredPeriod,
 *  monthlyDepositWon, acceptCondition, needLiquidity, goalAmountWon, goalMonths)
 */
export const postRecommend = (payload) =>
  axiosInstance.post("/api/recommend", payload).then(responseData);

/**
 * 추천 입력 폼 기본값. 나이·월소득·월 납입가능금액은 회원 정보와 소비내역에서 계산하고,
 * 나머지 추천 조건은 회원이 마지막으로 추천받을 때 저장한 값으로 가져온다.
 */
export const getRecommendDefaults = () =>
  axiosInstance.get("/api/recommend/defaults").then(responseData);
