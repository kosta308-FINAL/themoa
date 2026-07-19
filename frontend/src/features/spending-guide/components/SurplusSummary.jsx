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
          <span className="spending-status">
            저축 목표 {formatWon(data.savingsTargetAmount)}
          </span>
        )}
      </div>

      {data.completedCycleCount > 0 ? (
        <div className="spending-surplus-body">
          <div className="spending-surplus-main">
            <span>누적 잉여금</span>
            <strong className={surplusTone(data.totalSurplusAmount)}>
              {surplusSign(data.totalSurplusAmount)}
              {formatWon(data.totalSurplusAmount)}
            </strong>
            <p>완료된 {data.completedCycleCount}개 주기 합산</p>
          </div>
          {data.recentCycle && (
            <div className="spending-surplus-recent">
              <span>{monthLabel(data.recentCycle.yearMonth)} 주기</span>
              <strong className={surplusTone(data.recentCycle.amount)}>
                {surplusSign(data.recentCycle.amount)}
                {formatWon(data.recentCycle.amount)}
              </strong>
              <p>
                {toNumber(data.recentCycle.amount) < 0
                  ? "예산을 초과했어요"
                  : "예산보다 덜 썼어요"}
              </p>
            </div>
          )}
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

      {!data.hasSavingsGoal && (
        <div className="spending-surplus-goal-cta">
          <span className="spending-surplus-goal-icon">
            <DashboardIcon name="target" size={17} />
          </span>
          <div>
            <strong>아직 저축 목표가 없어요</strong>
            <p>목표를 정하면 잉여금을 목표 달성에 맞게 활용할 수 있어요.</p>
          </div>
          <button
            type="button"
            className="spending-primary"
            onClick={onSetGoal}
          >
            목표 정하기
          </button>
        </div>
      )}
    </section>
  );
}

export default SurplusSummary;

