import { Link } from "react-router-dom";
import { EMPLOYMENT_STATUS_LABELS } from "../hooks/usePolicyRecommendations";

function PolicyRecommendationProfileSummary({ profile }) {
  if (!profile) return null;
  const residence = [profile.residenceSido, profile.residenceSigungu]
    .filter(Boolean)
    .join(" ");
  const employment =
    EMPLOYMENT_STATUS_LABELS[profile.employmentStatus] || profile.employmentStatus;

  return (
    <div className="policy-recommendation-summary">
      <span>{residence}</span>
      <span>만 {profile.age}세</span>
      <span>{employment}</span>
      <Link to="/dashboard/mypage">추천 조건 수정</Link>
    </div>
  );
}

export default PolicyRecommendationProfileSummary;
