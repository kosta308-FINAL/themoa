import axiosInstance from "./axiosInstance";

const responseData = (response) => response.data?.data;

// 회원 관리 - 관리자 (personalinfo.md)
export const getAdminMembers = (params) =>
  axiosInstance.get("/api/admin/members", { params }).then(responseData);

export const getAdminMemberDetail = (memberId) =>
  axiosInstance
    .get(`/api/admin/members/${memberId}`)
    .then(responseData);

export const getAdminMemberActionLogs = (memberId, params) =>
  axiosInstance
    .get(`/api/admin/members/${memberId}/action-logs`, { params })
    .then(responseData);

export const lockAdminMember = (memberId, reason) =>
  axiosInstance
    .post(`/api/admin/members/${memberId}/lock`, { reason })
    .then(responseData);

export const unlockAdminMember = (memberId, reason) =>
  axiosInstance
    .post(`/api/admin/members/${memberId}/unlock`, { reason })
    .then(responseData);

export const forceLogoutAdminMember = (memberId, reason) =>
  axiosInstance
    .post(`/api/admin/members/${memberId}/force-logout`, { reason })
    .then(responseData);

export const withdrawAdminMember = (memberId, reason) =>
  axiosInstance
    .post(`/api/admin/members/${memberId}/withdraw`, { reason })
    .then(responseData);
