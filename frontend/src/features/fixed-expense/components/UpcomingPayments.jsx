import DashboardIcon from "../../../components/common/DashboardIcon";
import { formatAmount, paymentSchedule } from "../fixedExpenseUtils";

function UpcomingPayments({ items }) {
  const upcoming = items
    .map((item) => ({ item, schedule: paymentSchedule(item.expectedPayDay) }))
    .filter(({ schedule }) => schedule)
    .sort((a, b) => a.schedule.daysUntil - b.schedule.daysUntil)
    .slice(0, 5);

  return (
    <aside className="fx-panel fx-side-panel">
      <div className="fx-panel-head">
        <div className="fx-panel-title">
          <span className="fx-panel-title-icon fx-tone-blue">
            <DashboardIcon name="calendar" size={18} />
          </span>
          <div>
            <h2>다가오는 결제</h2>
            <p>다음 결제일을 미리 확인하세요.</p>
          </div>
        </div>
      </div>
      {upcoming.length ? (
        <div className="fx-schedule-list">
          {upcoming.map(({ item, schedule }) => (
            <div className="fx-schedule-item" key={item.id}>
              <span className="fx-date-box">
                <span>{schedule.date.getMonth() + 1}월</span>
                <strong>{schedule.date.getDate()}</strong>
              </span>
              <span className="fx-schedule-info">
                <strong>{item.name}</strong>
                <span>
                  {item.paymentMethod === "CARD" ? "카드" : "계좌이체"} ·{" "}
                  {schedule.daysUntil === 0 ? "오늘" : `${schedule.daysUntil}일 뒤`}
                </span>
              </span>
              <strong className="fx-schedule-amount">
                {formatAmount(item.expectedAmount, item.expectedCurrency)}
              </strong>
            </div>
          ))}
        </div>
      ) : (
        <div className="fx-empty-state">
          <p>등록된 고정지출이 없어요.</p>
        </div>
      )}
      <div className="fx-insight">
        <strong>
          <DashboardIcon name="info" size={15} />
          카드 연동 상태
        </strong>
        <p>
          카드 고정지출은 실제 결제내역과 자동으로 대조하고, 계좌이체는
          예정일만 알려드려요. 결제내역이 안 보이면 상세보기에서 확인할 수
          있어요.
        </p>
      </div>
    </aside>
  );
}

export default UpcomingPayments;
