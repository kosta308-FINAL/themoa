import { EMPLOYMENT_STATUS_LABELS } from "../hooks/usePolicyRecommendations";

function PolicyRecommendationProfileSummary({ profile }) {
  if (!profile) return null;
  const residence = [profile.residenceSido, profile.residenceSigungu]
    .filter(Boolean)
    .join(" ");
  const employment =
    EMPLOYMENT_STATUS_LABELS[profile.employmentStatus] ||
    profile.employmentStatus;

  return (
    <div className="policy-recommendation-summary">
      <span>{residence}</span>
      <span>만 {profile.age}세</span>
      <span>{employment}</span>
    </div>
  );
}

export default PolicyRecommendationProfileSummary;
