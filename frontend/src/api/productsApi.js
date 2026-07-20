import axiosInstance from "./axiosInstance";

const responseData = (response) => response.data?.data;

/**
 * 맞춤 금융상품 추천 요청. payload는 백엔드 RecommendRequest 형태를 따른다.
 * (age, monthlyIncomeManwon, employmentType, lowIncome, riskType, preferredPeriod,
 *  monthlyDepositWon, acceptCondition, needLiquidity, goalAmountWon, goalMonths)
 */
export const postRecommend = (payload) =>
  axiosInstance.post("/api/recommend", payload).then(responseData);
