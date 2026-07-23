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
 * 추천 입력 폼 기본값. 회원가입 월급과 소비내역 연동으로 쌓인 잉여금에서 가져온다.
 * monthlyIncomeManwon은 소비가이드 설정 전이면 null이고, monthlyDepositWon은 항상 값이 있다.
 */
export const getRecommendDefaults = () =>
  axiosInstance.get("/api/recommend/defaults").then(responseData);
