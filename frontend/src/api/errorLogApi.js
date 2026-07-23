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

export const getAdminLogFiles = (params) =>
  axiosInstance.get("/api/admin/logs/files", { params }).then(responseData);

export const getAdminApiPerformance = (params) =>
  axiosInstance
    .get("/api/admin/logs/api-performance", { params })
    .then(responseData);
