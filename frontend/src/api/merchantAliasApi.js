import axiosInstance from "./axiosInstance";

const responseData = (response) => response.data?.data;

export const searchMerchantAliases = (query) =>
  axiosInstance
    .get("/api/merchant-aliases", { params: query ? { q: query } : {} })
    .then(responseData);

// 가맹점 & 서비스 마스터 - 관리자
export const getPromotionCandidates = () =>
  axiosInstance
    .get("/api/admin/merchants/promotion-candidates")
    .then(responseData);

export const promoteMerchantAliasTerm = (aliasId, aliasText) =>
  axiosInstance.post("/api/admin/merchants/promotion-candidates/promote", {
    aliasId,
    aliasText,
  });

export const rejectMerchantAliasProposal = (aliasId, aliasText) =>
  axiosInstance.post("/api/admin/merchants/promotion-candidates/reject", {
    aliasId,
    aliasText,
  });

export const promoteMerchantAliasTermAsNewService = (
  aliasText,
  canonicalServiceName,
  categoryId,
) =>
  axiosInstance.post("/api/admin/merchants/promotion-candidates/promote-new", {
    aliasText,
    canonicalServiceName,
    categoryId,
  });

export const getUnclassifiedMerchants = () =>
  axiosInstance.get("/api/admin/merchants/unclassified").then(responseData);

export const registerQuickMerchantAlias = (
  merchantId,
  canonicalServiceName,
  categoryId,
) =>
  axiosInstance.post(`/api/admin/merchants/${merchantId}/quick-alias`, {
    canonicalServiceName,
    categoryId,
  });
