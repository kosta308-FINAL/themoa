import { useEffect, useState } from "react";
import { usePolicyBookmarks } from "../../hooks/usePolicyBookmarks";
import PolicyRecommendationProfileForm from "./PolicyRecommendationProfileForm";
import PolicyRecommendationProfileSummary from "./PolicyRecommendationProfileSummary";
import PolicyRecommendationTile from "./PolicyRecommendationTile";

const TWO_COLUMN_QUERY = "(min-width: 1101px)";

// 카드 펼침에 따라 높이가 계속 바뀌므로, CSS 멀티컬럼(자동 재배치)이 아니라
// 항목을 고정된 두 칸으로 직접 나눠서 펼쳐도 항목이 칸을 옮겨 다니지 않게 한다.
function useTwoColumnLayout() {
  const [isTwoColumn, setIsTwoColumn] = useState(
    () =>
      typeof window !== "undefined" &&
      window.matchMedia(TWO_COLUMN_QUERY).matches,
  );

  useEffect(() => {
    const mql = window.matchMedia(TWO_COLUMN_QUERY);
    const handleChange = (event) => setIsTwoColumn(event.matches);
    mql.addEventListener("change", handleChange);
    return () => mql.removeEventListener("change", handleChange);
  }, []);

  return isTwoColumn;
}

function PolicyRecommendationSection({
  recommendation,
  onSaveProfile,
  onRefresh,
}) {
  const bookmarks = usePolicyBookmarks();
  const isTwoColumn = useTwoColumnLayout();
  const [isEditingProfile, setIsEditingProfile] = useState(false);
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

  const isProfileUnchanged = (payload) =>
    (payload.residenceSido || "") === (profile?.residenceSido || "") &&
    (payload.residenceSigungu || null) ===
      (profile?.residenceSigungu || null) &&
    (payload.employmentStatus || "") === (profile?.employmentStatus || "");

  const handleProfileUpdate = async (payload) => {
    setIsEditingProfile(false);
    if (isProfileUnchanged(payload)) {
      return;
    }
    try {
      await onSaveProfile(payload);
    } catch {
      // 서버 오류 문구는 Hook의 mutationError로 표시한다.
    }
  };

  return (
    <section className="policy-recommendation-panel">
      <div className="policy-recommendation-head">
        <div>
          <h2>
            {profile?.configured
              ? "회원님을 위한 추천 정책"
              : "나에게 맞는 정책 추천받기"}
          </h2>
          <p>
            {profile?.configured
              ? "기본 조건과 관련성이 높은 정책입니다. 최종 신청 자격은 정책 상세 내용을 확인해주세요."
              : "기본 정보를 설정하면 회원님의 조건과 관련성이 높은 정책을 미리 찾아드려요."}
          </p>
        </div>
        <div className="policy-recommendation-head-actions">
          {profile?.configured && (
            <>
              <button
                type="button"
                className="policy-recommendation-edit"
                onClick={() => setIsEditingProfile(true)}
              >
                추천 조건 수정
              </button>
              <button
                type="button"
                className="policy-recommendation-refresh"
                disabled={isSaving}
                onClick={handleRefresh}
              >
                {isSaving ? "계산 중..." : "다시 추천받기"}
              </button>
            </>
          )}
        </div>
      </div>

      {isLoading && (
        <div className="policy-empty">추천 정보를 불러오는 중입니다.</div>
      )}
      {!isLoading && profileError && (
        <div className="policy-recommendation-state">
          <p>{profileError}</p>
          <button
            type="button"
            className="policy-primary-button"
            onClick={recommendation.load}
          >
            다시 불러오기
          </button>
        </div>
      )}
      {profileReady && regionError && (
        <div className="policy-recommendation-state">
          <p>{regionError}</p>
          <button
            type="button"
            className="policy-primary-button"
            onClick={recommendation.load}
          >
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
          <PolicyRecommendationProfileSummary
            profile={recommendations?.profile || profile}
          />
          {isSaving ? (
            <div
              className="policy-recommendation-recalculating"
              role="status"
              aria-live="polite"
            >
              <span className="policy-recommendation-recalculating-spinner" />
              <strong>변경된 조건에 맞춰 다시 추천해드려요</strong>
              <p>정책을 다시 계산하고 있어요. 잠시만 기다려주세요.</p>
            </div>
          ) : (
            <>
              {recommendationWarning && (
                <div className="policy-recommendation-warning">
                  <p>{recommendationWarning}</p>
                  <button
                    type="button"
                    className="policy-primary-button"
                    disabled={isSaving}
                    onClick={handleRefresh}
                  >
                    다시 추천받기
                  </button>
                </div>
              )}
              {mutationError && (
                <p className="policy-recommendation-error">{mutationError}</p>
              )}
              {recommendationError && (
                <div className="policy-recommendation-state">
                  <p>{recommendationError}</p>
                  <button
                    type="button"
                    className="policy-primary-button"
                    onClick={recommendation.load}
                  >
                    다시 불러오기
                  </button>
                </div>
              )}
              {!recommendationError && recommendations?.items?.length === 0 && (
                <div className="policy-empty">
                  현재 기본 조건과 일치하는 추천 정책이 없어요. 아래 자연어
                  검색에서 다른 조건으로 찾아볼 수 있어요.
                </div>
              )}
              {!recommendationError &&
                recommendations?.items?.length > 0 &&
                (() => {
                  const ranked = recommendations.items.map((item, index) => ({
                    item,
                    rank: index + 1,
                  }));
                  const renderTile = ({ item, rank }) => (
                    <PolicyRecommendationTile
                      key={item.policyId}
                      item={item}
                      rank={rank}
                      bookmarked={bookmarks.isBookmarked(item.policyId)}
                      bookmarkBusy={
                        bookmarks.loading ||
                        bookmarks.busyPolicyId === item.policyId
                      }
                      onBookmarkToggle={bookmarks.toggleBookmark}
                    />
                  );

                  if (!isTwoColumn) {
                    return (
                      <div className="policy-recommendation-list">
                        {ranked.map(renderTile)}
                      </div>
                    );
                  }

                  const leftColumn = ranked.filter((_, i) => i % 2 === 0);
                  const rightColumn = ranked.filter((_, i) => i % 2 === 1);
                  return (
                    <div className="policy-recommendation-columns">
                      <div className="policy-recommendation-column">
                        {leftColumn.map(renderTile)}
                      </div>
                      <div className="policy-recommendation-column">
                        {rightColumn.map(renderTile)}
                      </div>
                    </div>
                  );
                })()}
              {bookmarks.error && (
                <p className="policy-bookmark-error">{bookmarks.error}</p>
              )}
            </>
          )}
        </>
      )}

      {isEditingProfile && (
        <div
          className="policy-recommendation-modal-backdrop"
          role="presentation"
          onMouseDown={() => setIsEditingProfile(false)}
        >
          <section
            className="policy-recommendation-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="policy-recommendation-edit-title"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <div className="policy-recommendation-modal-head">
              <div>
                <h2 id="policy-recommendation-edit-title">추천 조건 수정</h2>
                <p>
                  거주지와 취업 상태를 변경하면 추천 정책을 다시 계산해드려요.
                </p>
              </div>
              <button
                type="button"
                className="policy-recommendation-modal-close"
                onClick={() => setIsEditingProfile(false)}
                aria-label="닫기"
              >
                ×
              </button>
            </div>
            <PolicyRecommendationProfileForm
              profile={profile}
              regions={regions}
              isSaving={isSaving}
              submitLabel="저장"
              onCancel={() => setIsEditingProfile(false)}
              onSubmit={handleProfileUpdate}
            />
            {mutationError && (
              <p className="policy-recommendation-error">{mutationError}</p>
            )}
          </section>
        </div>
      )}
    </section>
  );
}

export default PolicyRecommendationSection;
