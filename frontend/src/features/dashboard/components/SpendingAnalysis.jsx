import { Link } from "react-router-dom";
import { formatWon } from "../dashboardUtils";

const CATEGORY_COLORS = [
  "#16a34a",
  "#34d399",
  "#60a5fa",
  "#c084fc",
  "#fb923c",
  "#facc15",
];

function DonutChart({ categories }) {
  const radius = 60;
  const circumference = 2 * Math.PI * radius;
  const segments = categories.reduce((acc, cat) => {
    const dash = (cat.percentage / 100) * circumference;
    const previousOffset = acc.length === 0 ? 0 : acc[acc.length - 1].nextOffset;
    return [
      ...acc,
      {
        ...cat,
        dash,
        offset: previousOffset,
        nextOffset: previousOffset + dash,
      },
    ];
  }, []);

  return (
    <svg width="150" height="150" viewBox="0 0 150 150">
      {segments.map((cat) => (
        <circle
          key={cat.categoryName}
          cx="75"
          cy="75"
          r={radius}
          fill="none"
          stroke={cat.color}
          strokeWidth="18"
          strokeDasharray={`${cat.dash} ${circumference - cat.dash}`}
          strokeDashoffset={-cat.offset}
          transform="rotate(-90 75 75)"
        />
      ))}
    </svg>
  );
}

function SpendingAnalysis({ category, loading, error }) {
  const items = (category?.items || []).map((item, index) => ({
    ...item,
    color: CATEGORY_COLORS[index % CATEGORY_COLORS.length],
  }));

  return (
    <div className="widget-panel">
      <div className="widget-panel-header">
        <h3>소비 분석</h3>
        <Link to="/dashboard/spending/category-detail">더보기 &gt;</Link>
      </div>

      {loading && !category && <div className="dash-loading">소비 분석을 불러오고 있어요.</div>}
      {error && !category && <div className="dash-section-error">소비 분석을 불러오지 못했어요.</div>}
      {!loading && !error && items.length === 0 && (
        <div className="dash-empty-state">
          <strong>아직 분석할 소비 데이터가 없어요.</strong>
          <span>소비 내역이 쌓이면 카테고리별 비중을 확인할 수 있어요.</span>
        </div>
      )}
      {items.length > 0 && (
        <>
          {error && <div className="dash-section-error">{error}</div>}
          <div className="spending-chart-row">
            <div className="donut-wrap">
              <DonutChart categories={items} />
              <div className="donut-center">
                <strong>{formatWon(category.positiveNetTotal)}</strong>
                <span>이번 주기</span>
              </div>
            </div>
            <ul className="spending-legend">
              {items.map((cat) => (
                <li key={cat.categoryName}>
                  <span className="legend-dot" style={{ background: cat.color }} />
                  <span className="legend-label">{cat.categoryName}</span>
                  <span className="legend-percent">{cat.percentage}%</span>
                  <span className="legend-amount">{formatWon(cat.amount)}</span>
                </li>
              ))}
            </ul>
          </div>
        </>
      )}
    </div>
  );
}

export default SpendingAnalysis;
