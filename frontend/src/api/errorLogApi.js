import axiosInstance from "./axiosInstance";

const responseData = (response) => response.data?.data;

// 오류 로그 관리 - 관리자 (managelogging.md §5)
export const getAdminErrorLogs = (params) =>
  axiosInstance.get("/api/admin/logs/errors", { params }).then(responseData);

export const getAdminErrorLogDetail = (errorLogId) =>
  axiosInstance.get(`/api/admin/logs/errors/${errorLogId}`).then(responseData);

export const requestAdminErrorLogAiAnalysis = (errorLogId) =>
  axiosInstance
    .post(`/api/admin/logs/errors/${errorLogId}/ai-analyze`)
    .then(responseData);
