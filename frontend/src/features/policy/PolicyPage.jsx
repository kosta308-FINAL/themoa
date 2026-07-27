import { useState } from "react";
import DashboardIcon from "../../components/common/DashboardIcon";
import PolicyDetailPanel from "./components/PolicyDetailPanel";
import PolicySearchForm from "./components/PolicySearchForm";
import PolicySearchResults from "./components/PolicySearchResults";
import PolicySearchStatus from "./components/PolicySearchStatus";
import { usePolicySearch } from "./hooks/usePolicySearch";
import PolicyRecommendationSection from "./recommendation/components/PolicyRecommendationSection";
import { usePolicyRecommendations } from "./recommendation/hooks/usePolicyRecommendations";
import "./PolicyPage.css";

const examples = [
  "수원에 사는 27살 취업 준비생이 받을 수 있는 정책",
  "서울에 사는 대학생이 받을 수 있는 지원 정책",
  "대구에 사는 22살 청년이 받을 수 있는 금융 지원",
  "경기도에 사는 무직 청년이 신청할 수 있는 정책",
];

const MODE_RECOMMEND = "recommend";
const MODE_SEARCH = "search";

function PolicyPage() {
  const search = usePolicySearch("");
  const recommendation = usePolicyRecommendations();
  const [mode, setMode] = useState(MODE_RECOMMEND);

  const handleSaveRecommendationProfile = async (payload) => {
    await recommendation.saveProfile(payload);
  };

  const handleRefreshRecommendations = async () => {
    await recommendation.refresh();
  };

  return (
    <main className="dash-main policy-page">
      <div className="dash-topbar policy-topbar">
        <div>
          <h1>정책 지원</h1>
          <p>
            지역, 나이, 학생 여부와 취업 상태를 바탕으로 나에게 맞는 정책을
            찾아보세요.
          </p>
        </div>

        <div className="policy-mode-tabs" role="tablist">
          <button
            type="button"
            role="tab"
            aria-selected={mode === MODE_RECOMMEND}
            className={mode === MODE_RECOMMEND ? "active" : ""}
            onClick={() => setMode(MODE_RECOMMEND)}
          >
            <DashboardIcon name="target" size={15} />
            맞춤 추천
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={mode === MODE_SEARCH}
            className={mode === MODE_SEARCH ? "active" : ""}
            onClick={() => setMode(MODE_SEARCH)}
          >
            <DashboardIcon name="message" size={15} />
            AI 챗봇 검색
          </button>
        </div>
      </div>

      {mode === MODE_RECOMMEND ? (
        <PolicyRecommendationSection
          recommendation={recommendation}
          selected={search.selected}
          onSaveProfile={handleSaveRecommendationProfile}
          onRefresh={handleRefreshRecommendations}
          onOpenDetail={search.openDetail}
        />
      ) : (
        <>
          <PolicySearchForm
            query={search.query}
            examples={examples}
            loading={search.loading}
            totalText={search.totalText}
            onQueryChange={search.setQuery}
            onSearch={search.runSearch}
          />

          {search.error && <div className="policy-alert">{search.error}</div>}
          <PolicySearchStatus result={search.result} />

          {(search.loading || search.result) && (
            <section className="policy-content">
              <PolicySearchResults
                loading={search.loading}
                result={search.result}
                results={search.results}
                selected={search.selected}
                page={search.page}
                totalPages={search.totalPages}
                hasNextPage={search.hasNextPage}
                hasPreviousPage={search.hasPreviousPage}
                onPageChange={search.changePage}
                onOpenDetail={search.openDetail}
              />
            </section>
          )}
        </>
      )}

      <PolicyDetailPanel
        selected={search.selected}
        detailLoading={search.detailLoading}
        onClose={search.closeDetail}
      />
    </main>
  );
}

export default PolicyPage;
