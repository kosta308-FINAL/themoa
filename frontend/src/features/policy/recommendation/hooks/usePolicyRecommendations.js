import { useCallback, useEffect, useState } from "react";
import {
  createPolicyRecommendationProfile,
  getPolicyRecommendationProfile,
  getPolicyRecommendationRegions,
  getPolicyRecommendations,
  refreshPolicyRecommendations,
  updatePolicyRecommendationProfile,
} from "../../../../api/policyRecommendationApi";
import { getApiErrorMessage } from "../../../../utils/apiError";

export const EMPLOYMENT_STATUS_LABELS = {
  EMPLOYED: "재직 중",
  UNEMPLOYED: "미취업",
};

const RECOMMENDATION_WARNING_MESSAGE =
  "기본정보는 저장했지만 추천 정책을 계산하지 못했어요. 다시 추천받기를 눌러 재시도해 주세요.";

export function usePolicyRecommendations({ loadItems = true } = {}) {
  const [profile, setProfile] = useState(null);
  const [recommendations, setRecommendations] = useState(null);
  const [regions, setRegions] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [profileError, setProfileError] = useState("");
  const [regionError, setRegionError] = useState("");
  const [recommendationError, setRecommendationError] = useState("");
  const [mutationError, setMutationError] = useState("");
  const [recommendationWarning, setRecommendationWarning] = useState("");

  const load = useCallback(async () => {
    setIsLoading(true);
    setProfileError("");
    setRegionError("");
    setRecommendationError("");
    setRecommendationWarning("");
    const requests = [
      getPolicyRecommendationProfile(),
      getPolicyRecommendationRegions(),
    ];
    if (loadItems) {
      requests.push(getPolicyRecommendations());
    }
    const results = await Promise.allSettled(requests);
    if (results[0].status === "fulfilled") {
      setProfile(results[0].value);
    } else {
      setProfileError(getApiErrorMessage(results[0].reason, "정책 추천 기본정보를 불러오지 못했어요."));
    }
    if (results[1].status === "fulfilled") {
      setRegions(results[1].value?.items || []);
    } else {
      setRegions([]);
      setRegionError(getApiErrorMessage(results[1].reason, "지역 목록을 불러오지 못했어요."));
    }
    if (loadItems && results[2]?.status === "fulfilled") {
      setRecommendations(results[2].value);
    } else if (loadItems && results[2]?.status === "rejected") {
      setRecommendationError(getApiErrorMessage(results[2].reason, "추천 정책을 불러오지 못했어요."));
    }
    setIsLoading(false);
  }, [loadItems]);

  useEffect(() => {
    load();
  }, [load]);

  const saveProfile = async (payload) => {
    setIsSaving(true);
    setMutationError("");
    setRecommendationWarning("");
    try {
      const response = profile?.configured
        ? await updatePolicyRecommendationProfile(payload)
        : await createPolicyRecommendationProfile(payload);
      const nextProfile = response?.profile || response;
      setProfile(nextProfile);
      if (response?.recommendations) {
        setRecommendations(response.recommendations);
        setRecommendationError("");
      }
      if (response?.recommendationRefreshed === false) {
        setRecommendationWarning(RECOMMENDATION_WARNING_MESSAGE);
      }
      return response;
    } catch (saveError) {
      setMutationError(
        getApiErrorMessage(saveError, "정책 추천 기본정보를 저장하지 못했어요."),
      );
      throw saveError;
    } finally {
      setIsSaving(false);
    }
  };

  const refresh = async () => {
    setIsSaving(true);
    setMutationError("");
    setRecommendationWarning("");
    try {
      const response = await refreshPolicyRecommendations();
      setRecommendations(response);
      setRecommendationError("");
      return response;
    } catch (refreshError) {
      setMutationError(
        getApiErrorMessage(refreshError, "추천 정책을 다시 계산하지 못했어요."),
      );
      throw refreshError;
    } finally {
      setIsSaving(false);
    }
  };

  return {
    profile,
    recommendations,
    regions,
    isLoading,
    isSaving,
    error: profileError || regionError || recommendationError,
    profileError,
    regionError,
    recommendationError,
    mutationError,
    recommendationWarning,
    load,
    saveProfile,
    refresh,
  };
}
