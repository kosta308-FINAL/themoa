import DashboardIcon from "../../../../components/common/DashboardIcon";
import { EMPLOYMENT_STATUS_LABELS } from "../hooks/usePolicyRecommendations";

function PolicyRecommendationBanner({ profile, count, isLoading, onExpand }) {
  if (isLoading) {
    return (
      <button type="button" className="policy-recommendation-banner" disabled>
        <span>추천 조건을 확인하고 있어요...</span>
      </button>
    );
  }

  const configured = Boolean(profile?.configured);
  const residence = configured
    ? [profile.residenceSido, profile.residenceSigungu].filter(Boolean).join(" ")
    : "";
  const employmentLabel = configured ? EMPLOYMENT_STATUS_LABELS[profile.employmentStatus] : "";
  const conditionText = [residence && residence, profile?.age != null && `만 ${profile.age}세`, employmentLabel]
    .filter(Boolean)
    .join(" · ");

  const text = configured
    ? `${conditionText} 조건에 맞는 정책 ${count ?? 0}건 미리 보기`
    : "내 조건에 맞는 정책 추천받기";

  return (
    <button type="button" className="policy-recommendation-banner" onClick={onExpand}>
      <span>{text}</span>
      <DashboardIcon name="chevron-right" size={16} />
    </button>
  );
}

export default PolicyRecommendationBanner;
