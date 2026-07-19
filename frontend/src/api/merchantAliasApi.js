import axiosInstance from "./axiosInstance";

const responseData = (response) => response.data?.data;

export const searchMerchantAliases = (query) =>
  axiosInstance
    .get("/api/merchant-aliases", { params: query ? { q: query } : {} })
    .then(responseData);
