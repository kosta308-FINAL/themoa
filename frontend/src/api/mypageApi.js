import axiosInstance from "./axiosInstance";

const responseData = (response) => response.data?.data;

/**
 * 마이페이지 요약: 회원 정보 + 월 저축목표 + 약관 동의 이력.
 * 카드 연동 현황은 spendingGuideApi의 getCardConnections를, 저축목표 수정은
 * updateSavingsGoal을 그대로 쓴다(중복 구현하지 않음).
 */
export const getMyPage = () =>
  axiosInstance.get("/api/mypage").then(responseData);
