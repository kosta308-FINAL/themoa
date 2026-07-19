import DashboardIcon from "../../../components/common/DashboardIcon";
import {
  formatDate,
  formatDateWithWeekday,
  todayDate,
} from "../spendingGuideUtils";
import { PanelTitle } from "./SpendingGuideCommon";

function InitialSyncLoading({ compact = false, title, description }) {
  return (
    <div
      className={`spending-initial-loading${compact ? " compact" : ""}`}
      role="status"
    >
      <span className="spending-spinner" />
      <strong>{title}</strong>
      <p>{description}</p>
    </div>
  );
}

function InitialSyncView({ summary, syncState, retryingId, onRetry }) {
  const failed = syncState?.overallStatus === "FAILED";
  const failedConnections =
    syncState?.connections?.filter(
      (connection) => connection.initialSyncStatus === "FAILED",
    ) || [];
  const analyzing = syncState?.overallStatus === "ANALYZING";
  const loadingDescription = analyzing
    ? "불러온 거래를 정리하고 분석하는 중이에요."
    : "소비내역 수집이 끝나면 자동으로 표시돼요.";

  return (
    <>
      {failed && (
        <div className="spending-page-error spending-sync-error">
          <DashboardIcon name="info" size={19} />
          <span>
            카드 소비내역을 불러오지 못했어요. 다시 수집을 눌러 재시도해주세요.
          </span>
          {failedConnections.map((connection) => (
            <button
              type="button"
              key={connection.connectionId}
              disabled={retryingId === connection.connectionId}
              onClick={() => onRetry(connection.connectionId)}
            >
              {retryingId === connection.connectionId
                ? "재시도 중..."
                : `${connection.organizationName} 다시 수집`}
            </button>
          ))}
        </div>
      )}
      <section
        className="spending-hero spending-initial-sync"
        aria-label="카드 소비내역 초기 수집 상태"
      >
        <article className="spending-today-card">
          <div className="spending-card-head">
            <div>
              <h2>오늘의 소비 기준</h2>
              <p>
                {formatDateWithWeekday(todayDate())} · 오늘 하루 동안 권장액은
                유지돼요
              </p>
            </div>
            <span className={`spending-status${failed ? " warning" : ""}`}>
              {failed ? "수집 확인 필요" : "불러오는 중"}
            </span>
          </div>
          <InitialSyncLoading
            title={
              failed
                ? "소비 기준 계산을 기다리고 있어요"
                : "오늘 소비 기준을 계산하고 있어요"
            }
            description={
              failed
                ? "카드 내역을 다시 수집하면 자동으로 계산돼요."
                : loadingDescription
            }
          />
        </article>
        <aside className="spending-cycle-card">
          <span>이번 급여 주기</span>
          <p>
            {formatDate(summary?.cycleStartDate)} ~{" "}
            {formatDate(summary?.cycleEndDate)}
          </p>
          <InitialSyncLoading
            compact
            title="남은 예산을 계산하고 있어요"
            description="소비내역 수집이 끝나면 자동으로 표시돼요."
          />
        </aside>
      </section>
      <div className="spending-content-grid spending-initial-sync-grid">
        <div className="spending-column">
          <section className="spending-panel">
            <div className="spending-panel-head">
              <PanelTitle
                icon="receipt"
                title="오늘 거래"
                description="고정지출을 제외한 오늘의 거래를 보여드려요"
              />
            </div>
            <InitialSyncLoading
              title="최근 소비내역을 불러오고 있어요"
              description="화면을 나가도 수집은 계속 진행돼요."
            />
          </section>
          <section className="spending-panel">
            <div className="spending-panel-head">
              <PanelTitle
                icon="chart"
                title="최근 7일 소비 흐름"
                description="날짜별 순사용액과 하루 권장액을 비교해요"
                tone="blue"
              />
            </div>
            <InitialSyncLoading
              title="최근 소비 흐름을 만들고 있어요"
              description="날짜별 순사용액을 정리하는 중이에요."
            />
          </section>
        </div>
        <div className="spending-column">
          <section className="spending-panel">
            <div className="spending-panel-head">
              <PanelTitle
                icon="chart"
                title="카테고리별 소비"
                description="실제 소비 순액을 기준으로 보여드려요"
                tone="teal"
              />
            </div>
            <InitialSyncLoading
              title="카테고리를 분석하고 있어요"
              description="불러온 거래를 카테고리별로 정리하는 중이에요."
            />
          </section>
          <section className="spending-panel">
            <div className="spending-panel-head">
              <PanelTitle
                icon="sparkle"
                title="이번 달 이렇게 아껴봐요"
                description="지난 소비 습관을 바탕으로 알려드려요"
                tone="purple"
              />
            </div>
            <InitialSyncLoading
              title="소비 습관을 분석하고 있어요"
              description="분석할 내역이 충분한지 함께 확인하고 있어요."
            />
          </section>
        </div>
      </div>
    </>
  );
}

export default InitialSyncView;
