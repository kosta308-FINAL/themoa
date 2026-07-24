import AdminLayout from "../../components/layout/AdminLayout";
import BankLinkManager from "./components/BankLinkManager";
import CollectResultTable from "./components/CollectResultTable";
import FinancialStatusDashboard from "./components/FinancialStatusDashboard";
import SearchKeywordManager from "./components/SearchKeywordManager";
import PreferentialConditionManager from "./components/PreferentialConditionManager";
import SearchQualityInspector from "./components/SearchQualityInspector";
import { useFinancialProductAdmin } from "./hooks/useFinancialProductAdmin";
import { useFinancialProductStatus } from "./hooks/useFinancialProductStatus";
import "./FinancialProductAdminPage.css";

/** 수집 결과에서 신규로 들어온 상품 수(예·적금 + 대출). 인덱스 갱신이 필요한지 판단하는 기준. */
const insertedCount = (result) =>
  (result?.savings?.inserted || 0) + (result?.loans?.inserted || 0);

/**
 * 서버가 준 완료 시각(Asia/Seoul 기준 ISO)을 "14:35:41" 형태로 보여준다.
 * 오프셋이 없는 문자열이라 로컬 시각으로 파싱되지만, 그대로 다시 로컬로 표시하므로 숫자는 보존된다.
 */
const formatCompletedAt = (isoString) => {
  if (!isoString) {
    return "";
  }
  const completedAt = new Date(isoString);
  if (Number.isNaN(completedAt.getTime())) {
    return "";
  }
  return completedAt.toLocaleTimeString("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  });
};

function FinancialProductAdminPage() {
  const admin = useFinancialProductAdmin();
  const status = useFinancialProductStatus();
  const { collectResult, embedResult, running, error } = admin;

  const collecting = running === "collect";
  const rebuilding = running === "rebuild";
  const inserted = insertedCount(collectResult);
  const hasNewProducts = collectResult != null && inserted > 0;
  const collectedAt = formatCompletedAt(collectResult?.completedAt);
  const embeddedAt = formatCompletedAt(embedResult?.completedAt);

  // 실행이 끝나면 현황 숫자(상품 수·인덱스 문서 수·마지막 수집 시각)를 다시 맞춘다.
  const handleCollect = async () => {
    await admin.runCollect();
    await status.reload();
  };

  const handleRebuild = async () => {
    await admin.runRebuild();
    await status.reload();
  };

  // 인덱스가 판매중 상품 수와 어긋나 있거나 방금 신규 상품이 들어왔으면 갱신 버튼을 강조한다.
  const needsRebuild = status.indexSynced === false || hasNewProducts;

  return (
    <AdminLayout
      title="금융상품 관리"
      subtitle="금융감독원 API에서 상품을 수집하고 검색 인덱스를 갱신합니다."
    >
      <div className="fa-page">
        <FinancialStatusDashboard
          status={status.status}
          loading={status.loading}
          error={status.error}
          sellingTotal={status.sellingTotal}
          indexSynced={status.indexSynced}
        />

        {error && <div className="fa-alert fa-alert-danger">{error}</div>}

        {Boolean(running) && (
          <div className="fa-alert fa-alert-info">
            {collecting
              ? "상품을 수집하고 있어요. 수십 초에서 수 분까지 걸릴 수 있으니 이 화면을 닫지 말아 주세요."
              : "검색 인덱스를 갱신하고 있어요. 상품 수에 따라 수 분 걸릴 수 있어요."}
          </div>
        )}

        {/* 1단계: 수집 */}
        <section className="fa-card">
          <div className="fa-card-head">
            <div>
              <h2>1. 상품 수집</h2>
              <p>
                금융감독원 API에서 예·적금과 대출 상품을 받아와 DB에 반영합니다.
              </p>
            </div>
            <button
              type="button"
              className="admin-btn fa-btn-primary"
              onClick={handleCollect}
              disabled={Boolean(running)}
            >
              {collecting ? "수집 중…" : "상품 수집 실행"}
            </button>
          </div>

          {collectResult && (
            <>
              <CollectResultTable result={collectResult} />
              <div
                className={`fa-note ${hasNewProducts ? "fa-note-action" : ""}`}
              >
                {hasNewProducts
                  ? `신규 상품 ${inserted.toLocaleString()}건이 들어왔어요. 아래 "검색 인덱스 갱신"을 실행해 주세요.`
                  : "새로 들어온 상품이 없어 인덱스 갱신이 필요 없습니다."}
                {collectedAt && ` (${collectedAt} 완료)`}
              </div>
            </>
          )}
        </section>

        {/* 2단계: 인덱스 갱신 */}
        <section className="fa-card">
          <div className="fa-card-head">
            <div>
              <h2>2. 검색 인덱스 갱신</h2>
              <p>
                수집된 상품을 검색용 벡터 인덱스에 다시 임베딩합니다. 신규
                상품이 있을 때 실행하세요.
              </p>
            </div>
            <button
              type="button"
              className={`admin-btn fa-btn-primary${needsRebuild ? " fa-btn-attention" : ""}`}
              onClick={handleRebuild}
              disabled={Boolean(running)}
            >
              {rebuilding ? "갱신 중…" : "검색 인덱스 갱신"}
            </button>
          </div>

          {embedResult != null && (
            <div className="fa-note fa-note-done">
              인덱스 갱신 완료 — 총{" "}
              {Number(embedResult.embeddedCount ?? 0).toLocaleString()}건을
              임베딩했어요.
              {embeddedAt && ` (${embeddedAt} 완료)`}
            </div>
          )}
        </section>

        <BankLinkManager />

        <SearchQualityInspector />

        <SearchKeywordManager />
        <PreferentialConditionManager />
      </div>
    </AdminLayout>
  );
}

export default FinancialProductAdminPage;
