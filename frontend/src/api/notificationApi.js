import axiosInstance from "./axiosInstance";

const responseData = (response) => response.data?.data;

export const getNotifications = (params) =>
  axiosInstance.get("/api/notifications", { params }).then(responseData);

export const prepareDailyNotifications = (params) =>
  axiosInstance
    .post("/api/notifications/daily", null, { params })
    .then(responseData);

export const markNotificationRead = (notificationId) =>
  axiosInstance.patch(`/api/notifications/${notificationId}/read`);
