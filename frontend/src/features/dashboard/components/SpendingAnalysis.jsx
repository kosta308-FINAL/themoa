import { spendingBreakdown } from "../../../constants/mockDashboard";

function DonutChart({ categories }) {
  const radius = 60;
  const circumference = 2 * Math.PI * radius;
  let offsetAcc = 0;

  return (
    <svg width="150" height="150" viewBox="0 0 150 150">
      {categories.map((cat) => {
        const dash = (cat.percent / 100) * circumference;
        const circle = (
          <circle
            key={cat.label}
            cx="75"
            cy="75"
            r={radius}
            fill="none"
            stroke={cat.color}
            strokeWidth="18"
            strokeDasharray={`${dash} ${circumference - dash}`}
            strokeDashoffset={-offsetAcc}
            transform="rotate(-90 75 75)"
          />
        );
        offsetAcc += dash;
        return circle;
      })}
    </svg>
  );
}

function SpendingAnalysis() {
  return (
    <div className="widget-panel">
      <div className="widget-panel-header">
        <h3>소비 분석</h3>
        <a href="#">더보기 &gt;</a>
      </div>
      <div className="spending-chart-row">
        <div className="donut-wrap">
          <DonutChart categories={spendingBreakdown.categories} />
          <div className="donut-center">
            <strong>{spendingBreakdown.total}</strong>
            <span>{spendingBreakdown.totalLabel}</span>
          </div>
        </div>
        <ul className="spending-legend">
          {spendingBreakdown.categories.map((cat) => (
            <li key={cat.label}>
              <span className="legend-dot" style={{ background: cat.color }} />
              <span className="legend-label">{cat.label}</span>
              <span className="legend-percent">{cat.percent}%</span>
              <span className="legend-amount">{cat.amount}</span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

export default SpendingAnalysis;
