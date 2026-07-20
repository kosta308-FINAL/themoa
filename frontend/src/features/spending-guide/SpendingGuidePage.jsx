import { Link } from "react-router-dom";
import DashboardIcon from "../../components/common/DashboardIcon";
import BudgetSettingsModal from "./BudgetSettingsModal";
import CategorySummary from "./components/CategorySummary";
import CoachingPanel from "./components/CoachingPanel";
import FixedCandidates from "./components/FixedCandidates";
import InitialSyncView from "./components/InitialSyncView";
import RecentFlow from "./components/RecentFlow";
import SpendingGuideSetup from "./components/SpendingGuideSetup";
import { LoadingState, PanelTitle } from "./components/SpendingGuideCommon";
import SurplusSummary from "./components/SurplusSummary";
import TodayTransactions from "./components/TodayTransactions";
import useSpendingGuide from "./hooks/useSpendingGuide";
import IncomeAdjustmentModal from "./IncomeAdjustmentModal";
import ManualTransactionModal from "./ManualTransactionModal";
import {
  formatDate,
  formatDateWithWeekday,
  formatWon,
  INITIAL_SYNC_IN_PROGRESS,
  todayDate,
  toNumber,
} from "./spendingGuideUtils";
import TransactionDetailModal from "./TransactionDetailModal";
import "./SpendingGuidePage.css";

function SpendingGuidePage() {
  const {
    data,
    detailId,
    editingTransaction,
    expandToday,
    handleCardConnected,
    handleCategoryCycleChange,
    handleDismiss,
    handleInitialSyncRetry,
    handleManualSync,
    initialSyncState,
    isBudgetOpen,
    isEntryOpen,
    isIncomeAdjustmentOpen,
    isLoading,
    isSyncing,
    loadGuide,
    pageError,
    pendingCoachId,
    retryingConnectionId,
    sectionErrors,
    setDetailId,
    setEditingTransaction,
    setIsBudgetOpen,
    setIsEntryOpen,
    setIsIncomeAdjustmentOpen,
  } = useSpendingGuide();
  const summary = data.summary;
  const dailyRecommended = toNumber(summary?.dailyRecommendedAmount);
  const todaySpent = toNumber(summary?.todayNetSpend);
  const useRate =
    dailyRecommended > 0
      ? Math.max(0, Math.round((todaySpent / dailyRecommended) * 100))
      : 0;
  const cycleSpent = summary
    ? toNumber(summary.availableAmount) - toNumber(summary.remainingAmount)
    : 0;
  // surplus_fund(erd.md §6) 합계를 반환하는 조회 API가 아직 없어 누적 잉여금·최근 주기는
  // 빈 상태로 둔다(가짜 숫자 금지). 저축 목표 여부만 실제 summary.savingsGoalAmount로 판단한다.
  const surplusSummary = {
    hasSavingsGoal: toNumber(summary?.savingsGoalAmount) > 0,
    savingsTargetAmount: toNumber(summary?.savingsGoalAmount),
    completedCycleCount: 0,
    totalSurplusAmount: 0,
    recentCycle: null,
  };
  const showInitialSync =
    INITIAL_SYNC_IN_PROGRESS.has(initialSyncState?.overallStatus) ||
    initialSyncState?.overallStatus === "FAILED";
  const showSetup = summary?.setupRequired && !showInitialSync;
  const canManualSync = Boolean(
    data.connections?.cardSyncEnabled &&
    data.connections.connections?.some(
      (connection) => connection.connectionStatus === "ACTIVE",
    ),
  );

  return (
    <div className="spending-guide">
      <main className="spending-main">
        <header className="spending-page-head">
          <div>
            <h1>{showSetup ? "소비가이드 설정" : "소비가이드"}</h1>
            <p>
              {showSetup
                ? "처음 한 번만 입력하면 매일 소비 기준을 계산해드려요."
                : "오늘의 기준을 확인하고, 무리 없이 쓸 수 있는 금액을 관리해보세요."}
            </p>
          </div>
        </header>

        {pageError && (
          <div className="spending-page-error">
            <DashboardIcon name="info" size={19} />
            <span>{pageError}</span>
            <button type="button" onClick={loadGuide}>
              다시 시도
            </button>
          </div>
        )}
        {isLoading && !summary ? (
          <LoadingState label="소비가이드를 불러오고 있어요." />
        ) : showInitialSync ? (
          <InitialSyncView
            summary={summary}
            syncState={initialSyncState}
            retryingId={retryingConnectionId}
            onRetry={handleInitialSyncRetry}
          />
        ) : showSetup ? (
          <SpendingGuideSetup
            onComplete={loadGuide}
            onCardConnected={handleCardConnected}
          />
        ) : (
          summary && (
            <>
              <section className="spending-hero" aria-label="소비 기준 요약">
                <article className="spending-today-card">
                  <div className="spending-card-head">
                    <div>
                      <h2>오늘의 소비 기준</h2>
                      <p>
                        {formatDateWithWeekday(todayDate())} · 오늘 하루 동안
                        권장액은 유지돼요
                      </p>
                    </div>
                    <span
                      className={`spending-status ${useRate > 100 ? "warning" : ""}`}
                    >
                      {useRate > 100
                        ? "오늘 권장액 초과"
                        : "안정적으로 사용 중"}
                    </span>
                  </div>
                  <div className="spending-number-grid">
                    <div className="spending-number-main">
                      <span>
                        <DashboardIcon name="wallet" size={16} />
                        오늘 사용 가능 금액
                      </span>
                      <strong>{formatWon(summary.todayAvailableAmount)}</strong>
                      <p>
                        {toNumber(summary.todayAvailableAmount) <= 0
                          ? "오늘 권장액을 모두 사용했어요."
                          : "오늘 남은 권장 금액이에요."}
                      </p>
                    </div>
                    <div className="spending-mini-stat">
                      <span>하루 권장 소비액</span>
                      <strong>
                        {formatWon(summary.dailyRecommendedAmount)}
                      </strong>
                      <p>자정까지 고정</p>
                    </div>
                    <div className="spending-mini-stat">
                      <span>오늘 순사용액</span>
                      <strong>{formatWon(summary.todayNetSpend)}</strong>
                      <p>취소 반영 금액</p>
                    </div>
                  </div>
                  <div className="spending-progress-meta">
                    <span>오늘 권장액 사용률</span>
                    <strong>{useRate}%</strong>
                  </div>
                  <div className="spending-progress">
                    <i
                      className={useRate > 100 ? "over" : ""}
                      style={{
                        width: `${Math.min(100, Math.max(0, useRate))}%`,
                      }}
                    />
                  </div>
                  <div
                    className={`spending-today-message${useRate > 100 ? " warning" : ""}`}
                  >
                    <DashboardIcon
                      name={useRate > 100 ? "info" : "check"}
                      size={15}
                    />
                    {useRate > 100
                      ? `오늘 권장액을 ${formatWon(todaySpent - dailyRecommended)} 초과했어요.`
                      : "오늘 하루 동안 권장 범위 안에서 사용하고 있어요."}
                  </div>
                </article>
                <aside className="spending-cycle-card">
                  <div className="spending-cycle-top">
                    <span>이번 급여 주기</span>
                  </div>
                  <h2>남은 예산</h2>
                  <strong
                    className={
                      summary.overCycleBudget
                        ? "spending-cycle-amount negative"
                        : "spending-cycle-amount"
                    }
                  >
                    {formatWon(summary.remainingAmount)}
                  </strong>
                  <p>
                    {formatDate(summary.cycleStartDate)} ~{" "}
                    {formatDate(summary.cycleEndDate)}
                  </p>
                  <div className="spending-cycle-bottom">
                    <div>
                      <span>남은 기간</span>
                      <strong>{summary.remainingDays}일</strong>
                    </div>
                    <div>
                      <span>주기 순사용액</span>
                      <strong>{formatWon(cycleSpent)}</strong>
                    </div>
                  </div>
                </aside>
              </section>

              <div className="spending-content-grid">
                <div className="spending-column">
                  <SurplusSummary
                    data={surplusSummary}
                    onSetGoal={() => setIsBudgetOpen(true)}
                  />
                  <section className="spending-panel spending-transactions-panel">
                    <div className="spending-panel-head">
                      <PanelTitle
                        icon="receipt"
                        title="오늘 거래"
                        description="고정지출은 제외하고 보여드려요"
                      />
                      <div className="spending-panel-actions">
                        <button
                          type="button"
                          className="spending-secondary spending-sync-button"
                          onClick={handleManualSync}
                          disabled={!canManualSync || isSyncing}
                        >
                          <DashboardIcon name="repeat" size={15} />
                          {isSyncing ? "동기화 중..." : "카드내역 동기화"}
                        </button>
                        <button
                          type="button"
                          className="spending-secondary"
                          onClick={() => setIsEntryOpen(true)}
                          disabled={!data.categories?.length}
                        >
                          <DashboardIcon name="plus" size={15} />
                          지출 직접 입력
                        </button>
                        <button
                          type="button"
                          className="spending-secondary"
                          onClick={() => setIsIncomeAdjustmentOpen(true)}
                        >
                          <DashboardIcon name="plus" size={15} />
                          수입 직접 입력
                        </button>
                      </div>
                    </div>
                    <TodayTransactions
                      data={data.today}
                      error={sectionErrors.today}
                      onExpand={expandToday}
                      onSelect={setDetailId}
                    />
                  </section>
                  <section className="spending-panel spending-flow-panel">
                    <div className="spending-panel-head">
                      <PanelTitle
                        icon="chart"
                        title="최근 7일 소비 흐름"
                        description="날짜별 순사용액과 하루 권장액을 비교해요"
                        tone="blue"
                      />
                      <Link
                        className="spending-link-button"
                        to={`/dashboard/spending/transactions?date=${todayDate()}`}
                      >
                        상세보기{" "}
                        <DashboardIcon name="chevron-right" size={15} />
                      </Link>
                    </div>
                    <RecentFlow
                      data={data.recent}
                      error={sectionErrors.recent}
                    />
                  </section>
                </div>
                <div className="spending-column">
                  <section className="spending-panel spending-category-panel">
                    <div className="spending-panel-head">
                      <PanelTitle
                        icon="target"
                        title="카테고리별 소비"
                        description="실제 소비 순액 기준"
                        tone="teal"
                      />
                    </div>
                    <CategorySummary
                      data={data.category}
                      error={sectionErrors.category}
                      onNavigate={handleCategoryCycleChange}
                    />
                    <div className="spending-list-footer spending-category-footer">
                      <span />
                      <Link
                        className="spending-link-button"
                        to={`/dashboard/spending/category-detail${data.category?.budgetId ? `?budgetId=${data.category.budgetId}` : ""}`}
                      >
                        카테고리 상세보기{" "}
                        <DashboardIcon name="chevron-right" size={15} />
                      </Link>
                    </div>
                  </section>
                  <section className="spending-panel spending-fixed-candidate-panel">
                    <div className="spending-panel-head">
                      <PanelTitle
                        icon="repeat"
                        title="고정지출 후보"
                        description={
                          data.candidates?.length
                            ? `${data.candidates.length}개 중 우선순위 높은 ${Math.min(3, data.candidates.length)}개예요`
                            : "반복되는 결제를 찾아 알려드려요"
                        }
                        tone="orange"
                      />
                      {data.candidates?.length > 3 && (
                        <Link
                          className="spending-link-button"
                          to="/dashboard/fixed-expenses"
                        >
                          나머지 {data.candidates.length - 3}개{" "}
                          <DashboardIcon name="chevron-right" size={15} />
                        </Link>
                      )}
                    </div>
                    <FixedCandidates
                      data={data.candidates}
                      error={sectionErrors.candidates}
                    />
                  </section>
                  <CoachingPanel
                    data={data.coaching}
                    error={sectionErrors.coaching}
                    onDismiss={handleDismiss}
                    pendingId={pendingCoachId}
                  />
                </div>
              </div>
            </>
          )
        )}
      </main>
      {isEntryOpen && (
        <ManualTransactionModal
          categories={data.categories}
          allowCard={Boolean(
            data.connections &&
            (!data.connections.connections?.length ||
              !data.connections.cardSyncEnabled),
          )}
          onClose={() => setIsEntryOpen(false)}
          onSaved={loadGuide}
        />
      )}
      {isIncomeAdjustmentOpen && (
        <IncomeAdjustmentModal
          onClose={() => setIsIncomeAdjustmentOpen(false)}
          onSaved={loadGuide}
        />
      )}
      {isBudgetOpen && summary && (
        <BudgetSettingsModal
          summary={summary}
          onClose={() => setIsBudgetOpen(false)}
          onSaved={loadGuide}
        />
      )}
      {detailId && (
        <TransactionDetailModal
          transactionId={detailId}
          categories={data.categories}
          onClose={() => setDetailId(null)}
          onChanged={loadGuide}
          onEdit={(transaction) => {
            setDetailId(null);
            setEditingTransaction(transaction);
          }}
        />
      )}
      {editingTransaction && (
        <ManualTransactionModal
          transaction={editingTransaction}
          categories={data.categories}
          allowCard={Boolean(
            data.connections &&
            (!data.connections.connections?.length ||
              !data.connections.cardSyncEnabled),
          )}
          onClose={() => setEditingTransaction(null)}
          onSaved={loadGuide}
        />
      )}
    </div>
  );
}

export default SpendingGuidePage;
