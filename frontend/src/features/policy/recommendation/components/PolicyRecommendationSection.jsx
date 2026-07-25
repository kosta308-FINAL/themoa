import { usePolicyBookmarks } from "../../hooks/usePolicyBookmarks";
import PolicyRecommendationProfileForm from "./PolicyRecommendationProfileForm";
import PolicyRecommendationProfileSummary from "./PolicyRecommendationProfileSummary";
import PolicyRecommendationTile from "./PolicyRecommendationTile";

function PolicyRecommendationSection({
  recommendation,
  onSaveProfile,
  onRefresh,
  onOpenDetail,
  selected,
}) {
  const bookmarks = usePolicyBookmarks();
  const {
    profile,
    recommendations,
    regions,
    isLoading,
    isSaving,
    profileError,
    regionError,
    recommendationError,
    mutationError,
    recommendationWarning,
  } = recommendation;
  const profileReady = !isLoading && !profileError;
  const canShowProfileForm = profileReady && !regionError;

  const handleRefresh = async () => {
    try {
      await onRefresh();
    } catch {
      // 서버 오류 문구는 Hook의 mutationError로 표시한다.
    }
  };

  return (
    <section className="policy-recommendation-panel">
      <div className="policy-recommendation-head">
        <div>
          <h2>
            {profile?.configured ? "회원님을 위한 추천 정책" : "나에게 맞는 정책 추천받기"}
          </h2>
          <p>
            {profile?.configured
              ? "기본 조건과 관련성이 높은 정책입니다. 최종 신청 자격은 정책 상세 내용을 확인해주세요."
              : "기본 정보를 설정하면 회원님의 조건과 관련성이 높은 정책을 미리 찾아드려요."}
          </p>
        </div>
        {profile?.configured && (
          <button
            type="button"
            className="policy-recommendation-refresh"
            disabled={isSaving}
            onClick={handleRefresh}
          >
            {isSaving ? "계산 중..." : "다시 추천받기"}
          </button>
        )}
      </div>

      {isLoading && <div className="policy-empty">추천 정보를 불러오는 중입니다.</div>}
      {!isLoading && profileError && (
        <div className="policy-recommendation-state">
          <p>{profileError}</p>
          <button type="button" className="policy-primary-button" onClick={recommendation.load}>
            다시 불러오기
          </button>
        </div>
      )}
      {profileReady && regionError && (
        <div className="policy-recommendation-state">
          <p>{regionError}</p>
          <button type="button" className="policy-primary-button" onClick={recommendation.load}>
            다시 불러오기
          </button>
        </div>
      )}
      {canShowProfileForm && !profile?.configured && (
        <PolicyRecommendationProfileForm
          profile={profile}
          regions={regions}
          isSaving={isSaving}
          submitLabel="설정하고 추천받기"
          onSubmit={onSaveProfile}
        />
      )}
      {profileReady && profile?.configured && (
        <>
          <PolicyRecommendationProfileSummary profile={recommendations?.profile || profile} />
          {recommendationWarning && (
            <div className="policy-recommendation-warning">
              <p>{recommendationWarning}</p>
              <button
                type="button"
                className="policy-primary-button"
                disabled={isSaving}
                onClick={handleRefresh}
              >
                {isSaving ? "계산 중..." : "다시 추천받기"}
              </button>
            </div>
          )}
          {mutationError && <p className="policy-recommendation-error">{mutationError}</p>}
          {recommendationError && (
            <div className="policy-recommendation-state">
              <p>{recommendationError}</p>
              <button type="button" className="policy-primary-button" onClick={recommendation.load}>
                다시 불러오기
              </button>
            </div>
          )}
          {!recommendationError && recommendations?.items?.length === 0 && (
            <div className="policy-empty">
              현재 기본 조건과 일치하는 추천 정책이 없어요. 아래 자연어 검색에서 다른 조건으로 찾아볼 수 있어요.
            </div>
          )}
          {!recommendationError && recommendations?.items?.length > 0 && (
            <div className="policy-recommendation-grid">
              {recommendations.items.map((item) => (
                <PolicyRecommendationTile
                  key={item.policyId}
                  item={item}
                  active={selected?.policyId === item.policyId}
                  bookmarked={bookmarks.isBookmarked(item.policyId)}
                  bookmarkBusy={bookmarks.loading || bookmarks.busyPolicyId === item.policyId}
                  onBookmarkToggle={bookmarks.toggleBookmark}
                  onOpenDetail={onOpenDetail}
                />
              ))}
            </div>
          )}
          {bookmarks.error && <p className="policy-bookmark-error">{bookmarks.error}</p>}
        </>
      )}
    </section>
  );
}

export default PolicyRecommendationSection;
