import DashboardIcon from "../../../components/common/DashboardIcon";
import {
  formatAmount,
  METHOD_LABEL,
  paymentSchedule,
  paymentStatusBadge,
  scheduleBadge,
  serviceInitial,
  toneForId,
} from "../fixedExpenseUtils";

const SORT_OPTIONS = [
  { value: "payday", label: "결제일 가까운 순" },
  { value: "amount", label: "금액 높은 순" },
  { value: "name", label: "이름순" },
];

function sortItems(items, sort) {
  const withSchedule = items.map((item) => ({
    item,
    schedule: paymentSchedule(item.expectedPayDay),
  }));
  if (sort === "amount")
    return withSchedule.sort(
      (a, b) => b.item.expectedAmountKrw - a.item.expectedAmountKrw,
    );
  if (sort === "name")
    return withSchedule.sort((a, b) =>
      a.item.name.localeCompare(b.item.name, "ko"),
    );
  return withSchedule.sort(
    (a, b) => (a.schedule?.daysUntil ?? 99) - (b.schedule?.daysUntil ?? 99),
  );
}

function FixedExpenseList({
  items,
  filter,
  onFilterChange,
  sort,
  onSortChange,
  onSelect,
  onRegisterNew,
}) {
  const cardCount = items.filter(
    (item) => item.paymentMethod === "CARD",
  ).length;
  const transferCount = items.length - cardCount;
  const filtered =
    filter === "all"
      ? items
      : items.filter((item) => item.paymentMethod === filter.toUpperCase());
  const sorted = sortItems(filtered, sort);

  return (
    <section className="fx-panel">
      <div className="fx-panel-head">
        <div className="fx-panel-title">
          <span className="fx-panel-title-icon fx-tone-green">
            <DashboardIcon name="receipt" size={18} />
          </span>
          <div>
            <h2>내 고정지출</h2>
            <p>결제일이 가까운 순서로 보여드려요.</p>
          </div>
        </div>
        <button
          type="button"
          className="fx-primary-button"
          onClick={onRegisterNew}
        >
          <DashboardIcon name="plus" size={15} />
          고정지출 등록
        </button>
      </div>
      <div className="fx-list-toolbar">
        <div className="fx-filters">
          <button
            type="button"
            className={`fx-filter-chip${filter === "all" ? " active" : ""}`}
            onClick={() => onFilterChange("all")}
          >
            전체 {items.length}
          </button>
          <button
            type="button"
            className={`fx-filter-chip${filter === "card" ? " active" : ""}`}
            onClick={() => onFilterChange("card")}
          >
            카드 {cardCount}
          </button>
          <button
            type="button"
            className={`fx-filter-chip${filter === "transfer" ? " active" : ""}`}
            onClick={() => onFilterChange("transfer")}
          >
            계좌이체 {transferCount}
          </button>
        </div>
        <select
          className="fx-sort-select"
          aria-label="고정지출 정렬"
          value={sort}
          onChange={(event) => onSortChange(event.target.value)}
        >
          {SORT_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>
      {sorted.length ? (
        <div className="fx-expense-list">
          {sorted.map(({ item, schedule }) => {
            const badge = item.paymentStatus
              ? paymentStatusBadge(item.paymentStatus)
              : schedule
                ? scheduleBadge(schedule.daysUntil)
                : null;
            return (
              <button
                type="button"
                className="fx-expense-row"
                key={item.id}
                onClick={() => onSelect(item)}
              >
                <span className={`fx-service-icon ${toneForId(item.id)}`}>
                  {serviceInitial(item.merchantAliasName || item.name)}
                </span>
                <span className="fx-expense-info">
                  <span className="fx-expense-name">
                    <strong>{item.name}</strong>
                  </span>
                  <span className="fx-expense-meta">
                    {item.categoryName} · {METHOD_LABEL[item.paymentMethod]} ·
                    매월 {item.expectedPayDay}일
                  </span>
                </span>
                {badge && (
                  <span className={`fx-status-chip ${badge.tone}`}>
                    {badge.label}
                  </span>
                )}
                <span className="fx-expense-amount">
                  <strong>
                    {formatAmount(item.expectedAmount, item.expectedCurrency)}
                  </strong>
                  {item.expectedCurrency !== "KRW" && (
                    <span>
                      예상 {formatAmount(item.expectedAmountKrw, "KRW")}
                    </span>
                  )}
                </span>
                <span className="fx-chevron">
                  <DashboardIcon name="chevron-right" size={17} />
                </span>
              </button>
            );
          })}
        </div>
      ) : (
        <div className="fx-empty-state">
          <p>표시할 고정지출이 없어요.</p>
        </div>
      )}
    </section>
  );
}

export default FixedExpenseList;
