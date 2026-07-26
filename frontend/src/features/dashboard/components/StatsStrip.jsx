import DashboardIcon from "../../../components/common/DashboardIcon";
import DashEmptyState from "./DashEmptyState";
import DashSectionError from "./DashSectionError";
import { formatCount, formatWon, toNumber } from "../dashboardUtils";

function StatsStrip({
  summary,
  todayTransactionCount,
  loading,
  error,
}) {
  const summaryMissing = !summary || summary.setupRequired;

  if (loading && !summary) {
    return (
      <div className="stats-strip">
        <div className="dash-loading">개인 통계를 불러오고 있어요.</div>
      </div>
    );
  }

  if (summaryMissing) {
    return (
      <div className="stats-strip stats-strip-empty">
        <DashEmptyState
          icon="chart"
          title="아직 확인할 통계가 없어요"
          description="소비가이드를 설정하면 순사용액, 고정지출, 남은 일수를 한눈에 볼 수 있어요."
          action={{ label: "소비가이드 설정하기", to: "/dashboard/spending" }}
        />
      </div>
    );
  }

  const availableAmount = toNumber(summary?.availableAmount);
  const remainingAmount = toNumber(summary?.remainingAmount);
  const netSpend = availableAmount == null || remainingAmount == null
    ? null
    : availableAmount - remainingAmount;
  const stats = [
    {
      icon: "chart",
      label: "이번 주기 순사용액",
      value: formatWon(netSpend),
      unit: "",
    },
    {
      icon: "building",
      label: "예정 고정지출",
      value: formatWon(summary.expectedFixedExpenseTotal),
      unit: "",
    },
    {
      icon: "check",
      label: "주기 남은 일수",
      value: summary.remainingDays == null ? "-" : formatCount(summary.remainingDays, "일"),
      unit: "",
    },
    {
      icon: "sparkle",
      label: "오늘 거래",
      value: todayTransactionCount == null ? "-" : formatCount(todayTransactionCount, "건"),
      unit: "",
    },
  ];

  return (
    <div className="stats-strip">
      {stats.map((stat) => (
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
      {error && <DashSectionError message={error} />}
    </div>
  );
}

export default StatsStrip;
