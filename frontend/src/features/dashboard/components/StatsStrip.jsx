import DashboardIcon from "../../../components/common/DashboardIcon";
import { formatCount, formatWon, toNumber } from "../dashboardUtils";

function StatsStrip({
  summary,
  todayTransactionCount,
  loading,
  error,
}) {
  const availableAmount = toNumber(summary?.availableAmount);
  const remainingAmount = toNumber(summary?.remainingAmount);
  const netSpend = availableAmount == null || remainingAmount == null
    ? null
    : availableAmount - remainingAmount;
  const summaryMissing = !summary || summary.setupRequired;
  const stats = [
    {
      icon: "chart",
      label: "이번 주기 순사용액",
      value: summaryMissing ? "미설정" : formatWon(netSpend),
      unit: "",
    },
    {
      icon: "building",
      label: "예정 고정지출",
      value: summaryMissing ? "미설정" : formatWon(summary.expectedFixedExpenseTotal),
      unit: "",
    },
    {
      icon: "check",
      label: "주기 남은 일수",
      value: summaryMissing || summary.remainingDays == null
        ? "미설정"
        : formatCount(summary.remainingDays, "일"),
      unit: "",
    },
    {
      icon: "sparkle",
      label: "오늘 거래",
      value: todayTransactionCount == null ? "확인 불가" : formatCount(todayTransactionCount, "건"),
      unit: "",
    },
  ];

  return (
    <div className="stats-strip">
      {loading && !summary ? (
        <div className="dash-loading">개인 통계를 불러오고 있어요.</div>
      ) : stats.map((stat) => (
        <div className="stats-item" key={stat.label}>
          <span className="stats-icon">
            <DashboardIcon name={stat.icon} />
          </span>
          <div>
            <span className="stats-label">{stat.label}</span>
            <p>
              {stat.value}
              <span className="stats-unit">{stat.unit}</span>
            </p>
          </div>
        </div>
      ))}
      {error && <p className="dash-section-error">{error}</p>}
    </div>
  );
}

export default StatsStrip;
