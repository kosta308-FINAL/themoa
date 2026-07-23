import { useEffect, useMemo, useState } from "react";
import DashboardIcon from "../../../components/common/DashboardIcon";
import {
  formatDate,
  formatDateWithWeekday,
  todayDate,
} from "../spendingGuideUtils";
import { PanelTitle } from "./SpendingGuideCommon";

const COLLECTING_MESSAGES = [
  "카드사에서 소비내역을 불러오고 있어요.",
  "최초 동기화는 카드사 응답에 따라 3~5분 정도 걸릴 수 있어요.",
  "화면을 나가도 수집은 계속 진행되니 편하게 기다려주세요.",
  "수집이 끝나면 오늘의 소비 기준이 자동으로 계산돼요.",
];

const ANALYZING_MESSAGES = [
  "불러온 거래를 정리하고 분석하는 중이에요.",
  "카테고리별 소비와 소비 습관 분석도 함께 준비하고 있어요.",
  "조금만 더 기다려주시면 오늘의 소비 기준을 보여드릴게요.",
  "화면을 나가도 분석은 계속 진행돼요.",
];

const ROTATE_INTERVAL_MS = 3000;

function useRotatingMessage(messages) {
  const [tick, setTick] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => {
      setTick((current) => current + 1);
    }, ROTATE_INTERVAL_MS);
    return () => clearInterval(timer);
  }, []);

  return messages[tick % messages.length];
}

function InitialSyncOverlay({ analyzing }) {
  const messages = useMemo(
    () => (analyzing ? ANALYZING_MESSAGES : COLLECTING_MESSAGES),
    [analyzing],
  );
  const message = useRotatingMessage(messages);

  return (
    <section
      className="spending-initial-overlay"
      role="status"
      aria-live="polite"
    >
      <span className="spending-initial-overlay-spinner" />
      <strong>카드 소비내역을 동기화하고 있어요</strong>
      <p key={message} className="spending-initial-overlay-message">
        {message}
      </p>
    </section>
  );
}

function InitialSyncPlaceholder({ compact = false }) {
  return (
    <div
      className={`spending-initial-placeholder${compact ? " compact" : ""}`}
      aria-hidden="true"
    >
      <span className="spending-initial-placeholder-bar" />
      <span className="spending-initial-placeholder-bar short" />
    </div>
  );
}

function InitialSyncFailedNotice({ title, description }) {
  return (
    <div className="spending-initial-failed" role="status">
      <DashboardIcon name="info" size={20} />
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
      {!failed && <InitialSyncOverlay analyzing={analyzing} />}
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
          {failed ? (
            <InitialSyncFailedNotice
              title="소비 기준 계산을 기다리고 있어요"
              description="카드 내역을 다시 수집하면 자동으로 계산돼요."
            />
          ) : (
            <InitialSyncPlaceholder />
          )}
        </article>
        <aside className="spending-cycle-card">
          <span>이번 급여 주기</span>
          <p>
            {formatDate(summary?.cycleStartDate)} ~{" "}
            {formatDate(summary?.cycleEndDate)}
          </p>
          {failed ? (
            <InitialSyncFailedNotice
              title="남은 예산 계산 대기 중"
              description="카드 내역을 다시 수집하면 자동으로 계산돼요."
            />
          ) : (
            <InitialSyncPlaceholder compact />
          )}
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
            <InitialSyncPlaceholder />
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
            <InitialSyncPlaceholder />
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
            <InitialSyncPlaceholder />
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
            <InitialSyncPlaceholder compact />
          </section>
        </div>
      </div>
    </>
  );
}

export default InitialSyncView;
