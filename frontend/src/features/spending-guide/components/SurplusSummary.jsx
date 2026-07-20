import DashboardIcon from "../../../components/common/DashboardIcon";
import { formatWon, toNumber } from "../spendingGuideUtils";
import { EmptyState, PanelTitle } from "./SpendingGuideCommon";

function SurplusSummary({ data, onSetGoal }) {
  const surplusTone = (amount) => (toNumber(amount) < 0 ? "negative" : "");
  const surplusSign = (amount) => (toNumber(amount) > 0 ? "+" : "");
  const monthLabel = (yearMonth) =>
    yearMonth ? `${Number(yearMonth.slice(5, 7))}월` : "";

  return (
    <section
      className="spending-panel spending-surplus-panel"
      aria-label="잉여금 요약"
    >
      <div className="spending-panel-head">
        <PanelTitle
          icon="wallet"
          title="잉여금"
          description="예산보다 덜 쓴 금액이 급여 주기마다 쌓여요"
          tone="teal"
        />
        {data.hasSavingsGoal && (
          <div className="spending-surplus-goal-badge">
            <span className="spending-status">
              저축 목표 {formatWon(data.savingsTargetAmount)}
            </span>
            <button
              type="button"
              className="spending-surplus-goal-edit"
              onClick={onSetGoal}
              aria-label="저축 목표 수정"
            >
              <DashboardIcon name="edit" size={14} />
            </button>
          </div>
        )}
      </div>

      {data.ongoingCycle ? (
        <div className="spending-surplus-body">
          <div className="spending-surplus-main">
            <span>
              {monthLabel(data.ongoingCycle.yearMonth)} 주기 · 진행 중
            </span>
            <strong className={surplusTone(data.ongoingCycle.amount)}>
              {surplusSign(data.ongoingCycle.amount)}
              {formatWon(data.ongoingCycle.amount)}
            </strong>
            <p>
              {toNumber(data.ongoingCycle.amount) < 0
                ? "예산을 초과했어요"
                : "지금까지 예산보다 덜 썼어요 (주기가 끝나면 확정돼요)"}
            </p>
          </div>
          <div className="spending-surplus-recent">
            <span>완료된 주기 합산</span>
            {data.completedCycleCount > 0 ? (
              <>
                <strong className={surplusTone(data.totalSurplusAmount)}>
                  {surplusSign(data.totalSurplusAmount)}
                  {formatWon(data.totalSurplusAmount)}
                </strong>
                <p>완료된 {data.completedCycleCount}개 주기 합산</p>
              </>
            ) : (
              <>
                <strong>-</strong>
                <p>아직 끝난 주기가 없어요</p>
              </>
            )}
          </div>
        </div>
      ) : (
        <div className="spending-surplus-body">
          <EmptyState
            icon="wallet"
            title="아직 잉여금 데이터가 없어요"
            description="이번 급여 주기가 끝나면 잉여금이 계산돼요."
          />
        </div>
      )}
    </section>
  );
}

export default SurplusSummary;
