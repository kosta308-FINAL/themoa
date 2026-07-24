import { useState } from "react";
import DashboardIcon from "../../../components/common/DashboardIcon";
import PolicyRecommendationProfileForm from "../../policy/recommendation/components/PolicyRecommendationProfileForm";
import {
  EMPLOYMENT_STATUS_LABELS,
  usePolicyRecommendations,
} from "../../policy/recommendation/hooks/usePolicyRecommendations";

function PolicyRecommendationProfileCard({ onSaved }) {
  const [isEditing, setIsEditing] = useState(false);
  const recommendation = usePolicyRecommendations({ loadItems: false });
  const profile = recommendation.profile;
  const configured = profile?.configured;
  const residence = configured
    ? [profile.residenceSido, profile.residenceSigungu].filter(Boolean).join(" ")
    : "미설정";
  const employment = configured
    ? EMPLOYMENT_STATUS_LABELS[profile.employmentStatus] || profile.employmentStatus
    : "미설정";

  const handleSubmit = async (payload) => {
    const response = await recommendation.saveProfile(payload);
    setIsEditing(false);
    onSaved?.(
      response?.recommendationRefreshed === false
        ? "기본정보는 저장했지만 추천 정책을 계산하지 못했어요."
        : "정책 추천 기본정보를 저장했어요.",
    );
  };

  const handleRefresh = async () => {
    try {
      await recommendation.refresh();
      onSaved?.("추천 정책을 다시 계산했어요.");
    } catch {
      // 서버 오류 문구는 Hook의 mutationError로 표시한다.
    }
  };

  return (
    <section className="mp-card mp-policy-recommendation-card">
      <div className="mp-card-head">
        <span className="mp-card-icon">
          <DashboardIcon name="sparkle" size={17} />
        </span>
        <div className="mp-card-head-copy">
          <h2>정책 추천 기본정보</h2>
          <p className="mp-card-sub">생년월일 기준 나이는 자동 계산돼요.</p>
        </div>
        {configured && !isEditing && (
          <button
            type="button"
            className="mp-primary-button mp-card-head-action"
            onClick={() => setIsEditing(true)}
          >
            수정
          </button>
        )}
      </div>

      {recommendation.isLoading && (
        <p className="mp-empty">정책 추천 기본정보를 불러오고 있어요.</p>
      )}

      {!recommendation.isLoading && recommendation.profileError && (
        <div className="mp-policy-recommendation-error">
          <span>{recommendation.profileError}</span>
          <button type="button" onClick={recommendation.load}>
            다시 시도
          </button>
        </div>
      )}

      {!recommendation.isLoading && !recommendation.profileError && (
        <>
          <dl className="mp-info-list mp-policy-recommendation-summary">
            <div>
              <dt>적용 나이</dt>
              <dd>만 {profile?.age ?? "-"}세</dd>
            </div>
            <div>
              <dt>거주지</dt>
              <dd>{residence}</dd>
            </div>
            <div>
              <dt>취업 상태</dt>
              <dd>{employment}</dd>
            </div>
          </dl>
          {!configured && (
            <p className="mp-empty">아직 정책 추천 기본정보를 설정하지 않았어요.</p>
          )}
          {recommendation.regionError && (
            <div className="mp-policy-recommendation-error">
              <span>{recommendation.regionError}</span>
              <button type="button" onClick={recommendation.load}>
                다시 시도
              </button>
            </div>
          )}
          {recommendation.recommendationWarning && (
            <div className="mp-policy-recommendation-warning">
              <span>{recommendation.recommendationWarning}</span>
              <button type="button" disabled={recommendation.isSaving} onClick={handleRefresh}>
                {recommendation.isSaving ? "계산 중..." : "다시 추천받기"}
              </button>
            </div>
          )}
          {!recommendation.regionError && (!configured || isEditing) && (
            <PolicyRecommendationProfileForm
              profile={profile}
              regions={recommendation.regions}
              isSaving={recommendation.isSaving}
              submitLabel={configured ? "수정" : "기본정보 설정"}
              onCancel={configured ? () => setIsEditing(false) : undefined}
              onSubmit={handleSubmit}
            />
          )}
          {recommendation.mutationError && (
            <p className="mp-form-error">{recommendation.mutationError}</p>
          )}
        </>
      )}
    </section>
  );
}

export default PolicyRecommendationProfileCard;
