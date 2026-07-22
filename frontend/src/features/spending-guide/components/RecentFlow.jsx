import { Link } from "react-router-dom";
import {
  formatChartTick,
  formatDateWithWeekday,
  formatWon,
  todayDate,
  toNumber,
} from "../spendingGuideUtils";
import { EmptyState, LoadingState, SectionError } from "./SpendingGuideCommon";

function RecentFlow({ data, error }) {
  const values =
    data?.days?.map((day) => Math.abs(toNumber(day.netAmount))) || [];
  const rawMax = Math.max(toNumber(data?.guideLineAmount), ...values, 1);
  const axisStep = Math.max(10000, Math.ceil(rawMax / 4 / 10000) * 10000);
  const axisMax = axisStep * 4;
  const ticks = Array.from(
    { length: 5 },
    (_, index) => axisMax - axisStep * index,
  );
  const targetTop =
    12 +
    ((axisMax - Math.min(axisMax, toNumber(data?.guideLineAmount))) / axisMax) *
      160;
  if (error) return <SectionError message={error} />;
  if (!data) return <LoadingState />;
  if (
    !data.days?.length ||
    data.days.every((day) => toNumber(day.netAmount) === 0)
  )
    return (
      <EmptyState
        icon="chart"
        title="소비 흐름을 만들 데이터가 없어요"
        description="거래 데이터가 쌓이면 최근 7일 소비 흐름을 보여드려요."
      />
    );
  return (
    <>
      <div className="spending-chart">
        <div className="spending-chart-grid">
          {ticks.map((tick) => (
            <div className="spending-chart-line" key={tick}>
              <span>{formatChartTick(tick)}</span>
            </div>
          ))}
        </div>
        <div className="spending-guide-line" style={{ top: `${targetTop}px` }}>
          <span>오늘 권장 {formatWon(data.guideLineAmount)}</span>
        </div>
        <div className="spending-bars">
          {data.days.map((day) => {
            const amount = toNumber(day.netAmount);
            const isToday = day.date === todayDate();
            const guideLine = toNumber(data.guideLineAmount);
            const diff = guideLine - amount;
            return (
              <Link
                className="spending-bar-item"
                to={`/dashboard/spending/transactions?date=${day.date}`}
                key={day.date}
              >
                <span
                  className={`spending-bar-space${amount < 0 ? " negative-space" : ""}`}
                >
                  <i
                    className={`${amount < 0 ? "negative" : amount > guideLine ? "over" : ""}${isToday ? " today" : ""}`}
                    style={{
                      height: `${Math.max(5, (Math.abs(amount) / axisMax) * 100)}%`,
                    }}
                  >
                    <span className="spending-bar-tooltip">
                      <strong>{formatDateWithWeekday(day.date)}</strong>
                      <span>
                        {amount < 0 ? "취소 반영 순사용액" : "순사용액"}{" "}
                        {formatWon(Math.abs(amount))}
                      </span>
                      <span>
                        하루 권장액 {formatWon(guideLine)} 대비{" "}
                        {diff >= 0
                          ? `${formatWon(diff)} 여유`
                          : `${formatWon(-diff)} 초과`}
                      </span>
                    </span>
                  </i>
                </span>
                <strong>
                  {isToday ? "오늘" : `${Number(day.date.slice(8, 10))}일`}
                </strong>
                <em className="spending-bar-amount">
                  {formatChartTick(Math.abs(amount))}
                </em>
              </Link>
            );
          })}
        </div>
      </div>
      <div className="spending-chart-legend">
        <i />
        하늘색 막대는 취소금액이 사용액보다 큰 날이에요
      </div>
    </>
  );
}

export default RecentFlow;
