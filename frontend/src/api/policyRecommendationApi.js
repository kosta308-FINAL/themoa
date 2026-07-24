import axiosInstance from "./axiosInstance";

const responseData = (response) => response.data?.data;

export const getPolicyRecommendationProfile = () =>
  axiosInstance.get("/api/policy-recommendations/profile").then(responseData);

export const createPolicyRecommendationProfile = (payload) =>
  axiosInstance
    .post("/api/policy-recommendations/profile", payload)
    .then(responseData);

export const updatePolicyRecommendationProfile = (payload) =>
  axiosInstance
    .patch("/api/policy-recommendations/profile", payload)
    .then(responseData);

export const getPolicyRecommendations = () =>
  axiosInstance.get("/api/policy-recommendations").then(responseData);

export const refreshPolicyRecommendations = () =>
  axiosInstance.post("/api/policy-recommendations/refresh").then(responseData);

export const getPolicyRecommendationRegions = () =>
  axiosInstance.get("/api/policy-recommendations/regions").then(responseData);
