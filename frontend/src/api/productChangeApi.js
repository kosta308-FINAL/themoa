import axiosInstance from "./axiosInstance";

const responseData = (response) => response.data?.data;

/** 알림 하나에 대응하는 관심 상품 변경 내역(전/후 + 이전 이력). */
export const getProductChangeByNotification = (notificationId) =>
  axiosInstance
    .get(`/api/financial-products/changes/by-notification/${notificationId}`)
    .then(responseData);

/** 내 관심 상품의 변경 내역 목록(최근순). */
export const getProductChanges = () =>
  axiosInstance.get("/api/financial-products/changes").then(responseData);
