import { useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import "./CategoryDetailPage.css";

// 급여주기별 카테고리 이력·시점 분석 API가 아직 없어 임시 mock.
// 실제 연동 시 getCategoryDetail(categoryId)류 API로 교체 예정.
const CATEGORIES = [
  {
    id: "food",
    name: "식비",
    color: "#22c55e",
    history: [116000, 128000, 137000, 161000],
    phases: [
      [34, 28, 38],
      [29, 25, 46],
    ],
    weekdayRatios: [0.61, 0.58],
  },
  {
    id: "cafe",
    name: "카페/간식",
    color: "#14b8a6",
    history: [98000, 112000, 126000, 109000],
    phases: [
      [31, 35, 34],
      [38, 34, 28],
    ],
    weekdayRatios: [0.66, 0.68],
  },
  {
    id: "transport",
    name: "교통",
    color: "#60a5fa",
    history: [72000, 73500, 76000, 80500],
    phases: [
      [33, 34, 33],
      [35, 37, 28],
    ],
    weekdayRatios: [0.82, 0.84],
  },
  {
    id: "shopping",
    name: "쇼핑",
    color: "#f59e0b",
    history: [54000, 46500, 32000, 71100],
    phases: [
      [43, 25, 32],
      [18, 27, 55],
    ],
    weekdayRatios: [0.57, 0.52],
  },
  {
    id: "etc",
    name: "기타",
    color: "#a78bfa",
    history: [47000, 56000, 61000, 52000],
    phases: [
      [36, 34, 30],
      [32, 30, 38],
    ],
    weekdayRatios: [0.63, 0.6],
  },
];

const CYCLE_META = [
  { label: "7월 급여주기", range: "7.5 ~ 오늘 · 진행 중", index: 3 },
  { label: "6월 급여주기", range: "6.5 ~ 7.4 · 완료", index: 2 },
];
const CYCLE_LABELS = ["4월", "5월", "6월", "7월"];
const PHASE_NAMES = ["초반", "중반", "후반"];

const formatWon = (value) => `${Math.round(value).toLocaleString("ko-KR")}원`;
const formatDelta = (value) =>
  `${value > 0 ? "+" : ""}${Math.round(value).toLocaleString("ko-KR")}원`;

const ICONS = {
  chart: <path d="M4 20V10m6 10V4m6 16v-7m5 7H2" />,
  target: (
    <>
      <circle cx="12" cy="12" r="9" />
      <circle cx="12" cy="12" r="5" />
      <circle cx="12" cy="12" r="1" />
    </>
  ),
  trend: (
    <>
      <path d="m4 16 5-5 4 4 7-8" />
      <path d="M15 7h5v5" />
    </>
  ),
  calendar: (
    <>
      <rect x="3" y="5" width="18" height="16" rx="2" />
      <path d="M16 3v4M8 3v4M3 10h18" />
    </>
  ),
  sparkle: (
    <>
      <path d="m12 3 1.5 4.2L18 9l-4.5 1.8L12 15l-1.5-4.2L6 9l4.5-1.8L12 3Z" />
      <path d="m19 15 .8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8L19 15Z" />
    </>
  ),
  "chevron-left": <path d="m15 18-6-6 6-6" />,
  "chevron-right": <path d="m9 18 6-6-6-6" />,
  "arrow-left": <path d="M19 12H5m6-6-6 6 6 6" />,
  info: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 11v5m0-8h.01" />
    </>
  ),
};

function Icon({ name, small = false }) {
  return (
    <svg
      className={`icon${small ? " icon-sm" : ""}`}
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
      {ICONS[name]}
    </svg>
  );
}

function CategoryDetailPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [cycleOffset, setCycleOffset] = useState(0);
  const [selectedId, setSelectedId] = useState(
    () =>
      CATEGORIES.find((item) => item.name === searchParams.get("category"))
        ?.id || "food",
  );

  const cycle = CYCLE_META[cycleOffset];
  const index = cycle.index;
  const phaseIndex = cycleOffset === 0 ? 1 : 0;
  const category = CATEGORIES.find((item) => item.id === selectedId);

  const selectCategory = (id) => {
    setSelectedId(id);
    const nextCategory = CATEGORIES.find((item) => item.id === id);
    setSearchParams({ category: nextCategory.name }, { replace: true });
  };

  const comparisonRows = useMemo(() => {
    const total = CATEGORIES.reduce((sum, item) => sum + item.history[index], 0);
    const max = Math.max(
      ...CATEGORIES.flatMap((item) => [
        item.history[index],
        item.history[index - 1],
      ]),
    );
    return CATEGORIES.map((item) => {
      const current = item.history[index];
      const previous = item.history[index - 1];
      return {
        ...item,
        current,
        previous,
        delta: current - previous,
        share: Math.round((current / total) * 100),
        max,
      };
    });
  }, [index]);

  const trend = useMemo(() => {
    const values = category.history;
    const width = 640;
    const height = 250;
    const left = 54;
    const right = 28;
    const top = 34;
    const bottom = 44;
    const max = Math.ceil(Math.max(...values) / 50000) * 50000;
    const plotWidth = width - left - right;
    const plotHeight = height - top - bottom;
    const points = values.map((value, i) => ({
      x: left + (plotWidth / (values.length - 1)) * i,
      y: top + ((max - value) / max) * plotHeight,
      value,
    }));
    const linePoints = points.map((p) => `${p.x},${p.y}`).join(" ");
    const areaPoints = `${left},${top + plotHeight} ${linePoints} ${left + plotWidth},${top + plotHeight}`;
    const grid = [0, 0.5, 1].map((rate) => {
      const y = top + plotHeight * rate;
      const value = max * (1 - rate);
      return {
        y,
        label: value === 0 ? "0" : `${Math.round(value / 10000)}만`,
      };
    });
    return { width, height, left, right, top, points, linePoints, areaPoints, grid };
  }, [category]);

  const phases = category.phases[phaseIndex];
  const weekdayRatio = category.weekdayRatios[phaseIndex];
  const currentAmount = category.history[index];
  const weekdayAmount = Math.round((currentAmount * weekdayRatio) / 100) * 100;
  const weekendAmount = currentAmount - weekdayAmount;

  const insights = useMemo(() => {
    const current = category.history[index];
    const previous = category.history[index - 1];
    const delta = current - previous;
    const deltaRate = Math.round((Math.abs(delta) / previous) * 100);
    const maxPhaseIndex = phases.indexOf(Math.max(...phases));
    const lastThree = category.history.slice(Math.max(0, index - 2), index + 1);
    const increasing = lastThree.every((v, i) => i === 0 || v > lastThree[i - 1]);
    const decreasing = lastThree.every((v, i) => i === 0 || v < lastThree[i - 1]);
    const trendTitle = increasing
      ? "연속해서 증가하고 있어요"
      : decreasing
        ? "연속해서 감소하고 있어요"
        : "급여주기마다 변동이 있어요";
    const trendBody = increasing
      ? `${category.name} 소비가 최근 ${lastThree.length}번의 급여주기 동안 계속 늘었어요.`
      : decreasing
        ? `${category.name} 소비가 최근 ${lastThree.length}번의 급여주기 동안 계속 줄었어요.`
        : `${category.name} 소비가 일정한 방향보다 급여주기별로 다르게 움직이고 있어요.`;
    return [
      {
        title: delta >= 0 ? "이전보다 소비가 늘었어요" : "이전보다 소비가 줄었어요",
        body: `${category.name} 소비가 이전 급여주기보다 ${formatWon(Math.abs(delta))}, 약 ${deltaRate}% ${delta >= 0 ? "증가했어요" : "감소했어요"}.`,
      },
      {
        title: `${PHASE_NAMES[maxPhaseIndex]}에 가장 많이 사용했어요`,
        body: `${category.name} 소비의 ${phases[maxPhaseIndex]}%가 급여주기 ${PHASE_NAMES[maxPhaseIndex]} 구간에 발생했어요.`,
      },
      { title: trendTitle, body: trendBody },
    ];
  }, [category, index, phases]);

  return (
    <div className="category-detail-page" style={{ "--accent": category.color }}>
      <main className="page">
        <Link className="back-link" to="/dashboard/spending">
          <Icon name="arrow-left" />
          소비가이드로 돌아가기
        </Link>

        <div className="page-head">
          <div>
            <div className="eyebrow">
              <Icon name="target" small />
              CATEGORY ANALYSIS
            </div>
            <h1>카테고리 소비 상세</h1>
            <p>카테고리별 변화와 소비 시점을 비교해 지출 흐름을 확인해보세요.</p>
          </div>
          <div className="cycle-nav" aria-label="급여주기 선택">
            <button
              type="button"
              aria-label="이전 급여주기"
              onClick={() =>
                setCycleOffset((current) =>
                  Math.min(CYCLE_META.length - 1, current + 1),
                )
              }
              disabled={cycleOffset === CYCLE_META.length - 1}
            >
              <Icon name="chevron-left" small />
            </button>
            <div className="cycle-copy">
              <strong>{cycle.label}</strong>
              <span>{cycle.range}</span>
            </div>
            <button
              type="button"
              aria-label="다음 급여주기"
              onClick={() =>
                setCycleOffset((current) => Math.max(0, current - 1))
              }
              disabled={cycleOffset === 0}
            >
              <Icon name="chevron-right" small />
            </button>
          </div>
        </div>

        <section className="panel" aria-labelledby="comparisonTitle">
          <div className="panel-head">
            <div className="panel-title">
              <span className="badge-icon">
                <Icon name="chart" />
              </span>
              <div>
                <h2 id="comparisonTitle">카테고리별 변화</h2>
                <p>이전 급여주기와 비교해 소비가 어디에서 달라졌는지 보여드려요</p>
              </div>
            </div>
            <div className="comparison-legend" aria-label="비교 범례">
              <span>
                <i />
                이번 주기
              </span>
              <span>
                <i />
                이전 주기
              </span>
            </div>
          </div>
          <div className="category-table-head">
            <span>카테고리</span>
            <span>소비 규모</span>
            <span>이번 주기</span>
            <span>변화</span>
          </div>
          <div>
            {comparisonRows.map((row) => (
              <button
                type="button"
                key={row.id}
                className={`category-row${row.id === selectedId ? " active" : ""}`}
                style={{ "--accent": row.color }}
                onClick={() => selectCategory(row.id)}
              >
                <span className="category-name">
                  <i className="category-dot" />
                  <span>
                    {row.name}
                    <small>전체 소비의 {row.share}%</small>
                  </span>
                </span>
                <span className="bar-pair">
                  <i className="bar-track">
                    <i
                      className="bar-fill"
                      style={{ width: `${(row.current / row.max) * 100}%` }}
                    />
                  </i>
                  <i className="bar-track">
                    <i
                      className="bar-fill previous"
                      style={{ width: `${(row.previous / row.max) * 100}%` }}
                    />
                  </i>
                </span>
                <span className="amount-cell">
                  <strong>{formatWon(row.current)}</strong>
                  <span>이전 {formatWon(row.previous)}</span>
                </span>
                <span className="delta-cell">
                  <span className={`delta ${row.delta >= 0 ? "up" : "down"}`}>
                    {row.delta >= 0 ? "↑" : "↓"} {formatDelta(row.delta)}
                  </span>
                </span>
              </button>
            ))}
          </div>
        </section>

        <div className="detail-tabs" aria-label="분석할 카테고리 선택">
          {CATEGORIES.map((item) => (
            <button
              type="button"
              key={item.id}
              className={`category-tab${item.id === selectedId ? " active" : ""}`}
              style={{ "--accent": item.color }}
              onClick={() => selectCategory(item.id)}
            >
              {item.name}
            </button>
          ))}
        </div>

        <div className="analysis-grid">
          <section className="panel" aria-labelledby="trendTitle">
            <div className="panel-head">
              <div className="panel-title">
                <span
                  className="badge-icon"
                  style={{ color: "#2769c5", background: "var(--blue-soft)" }}
                >
                  <Icon name="trend" />
                </span>
                <div>
                  <h2 id="trendTitle">급여주기별 소비 흐름</h2>
                  <p>최근 네 번의 급여주기에서 선택한 카테고리의 변화를 확인해요</p>
                </div>
              </div>
              <span
                className="selected-category"
                style={{
                  color: category.color,
                  background: `color-mix(in srgb, ${category.color} 12%, white)`,
                }}
              >
                {category.name}
              </span>
            </div>
            <svg
              className="trend-chart"
              viewBox="0 0 640 250"
              role="img"
              aria-label="선택 카테고리의 급여주기별 소비 추이"
            >
              <defs>
                <linearGradient id="trendGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor={category.color} stopOpacity=".20" />
                  <stop offset="100%" stopColor={category.color} stopOpacity=".01" />
                </linearGradient>
              </defs>
              {trend.grid.map((g, i) => (
                <g key={i}>
                  <line
                    className="grid-line"
                    x1={trend.left}
                    y1={g.y}
                    x2={trend.width - trend.right}
                    y2={g.y}
                  />
                  <text
                    className="chart-label"
                    x={trend.left - 9}
                    y={g.y + 3}
                    textAnchor="end"
                  >
                    {g.label}
                  </text>
                </g>
              ))}
              <polygon className="trend-area" points={trend.areaPoints} />
              <polyline className="trend-line" points={trend.linePoints} />
              {trend.points.map((p, i) => (
                <g key={i}>
                  <circle className="trend-point" cx={p.x} cy={p.y} r="5" />
                  <text
                    className="chart-value"
                    x={p.x}
                    y={p.y - 13}
                    textAnchor="middle"
                  >
                    {Math.round(p.value / 1000).toLocaleString()}천
                  </text>
                  <text
                    className="chart-label"
                    x={p.x}
                    y={trend.height - 15}
                    textAnchor="middle"
                  >
                    {CYCLE_LABELS[i]} 급여주기
                  </text>
                </g>
              ))}
            </svg>
          </section>

          <section className="panel" aria-labelledby="timingTitle">
            <div className="panel-head">
              <div className="panel-title">
                <span
                  className="badge-icon"
                  style={{ color: "#734bd1", background: "var(--purple-soft)" }}
                >
                  <Icon name="calendar" />
                </span>
                <div>
                  <h2 id="timingTitle">급여주기 내 소비 시점</h2>
                  <p>선택한 카테고리의 소비가 언제 집중됐는지 보여드려요</p>
                </div>
              </div>
            </div>
            <div className="timing-stack">
              <div>
                <div className="subsection-title">
                  <strong>{category.name} 소비 시점</strong>
                  <span>주기 진행 구간 기준</span>
                </div>
                <div className="phase-bar" aria-label="급여주기 구간별 소비 비중">
                  <span className="early" style={{ width: `${phases[0]}%` }} />
                  <span className="middle" style={{ width: `${phases[1]}%` }} />
                  <span className="late" style={{ width: `${phases[2]}%` }} />
                </div>
                <div className="phase-list">
                  <div className="phase-item">
                    <span>초반 · 1~10일</span>
                    <strong>{phases[0]}%</strong>
                  </div>
                  <div className="phase-item">
                    <span>중반 · 11~20일</span>
                    <strong>{phases[1]}%</strong>
                  </div>
                  <div className="phase-item">
                    <span>후반 · 21일 이후</span>
                    <strong>{phases[2]}%</strong>
                  </div>
                </div>
              </div>
              <div>
                <div className="subsection-title">
                  <strong>평일·주말 비교</strong>
                  <span>결제일 기준</span>
                </div>
                <div className="weekday-row">
                  <span>평일</span>
                  <div className="weekday-track">
                    <div
                      className="weekday-fill"
                      style={{ width: `${weekdayRatio * 100}%` }}
                    />
                  </div>
                  <strong>{formatWon(weekdayAmount)}</strong>
                </div>
                <div className="weekday-row">
                  <span>주말</span>
                  <div className="weekday-track">
                    <div
                      className="weekday-fill weekend"
                      style={{ width: `${(1 - weekdayRatio) * 100}%` }}
                    />
                  </div>
                  <strong>{formatWon(weekendAmount)}</strong>
                </div>
              </div>
            </div>
          </section>
        </div>

        <section className="panel insight-panel" aria-labelledby="insightTitle">
          <div className="panel-head">
            <div className="panel-title">
              <span
                className="badge-icon"
                style={{ color: "#a96700", background: "var(--orange-soft)" }}
              >
                <Icon name="sparkle" />
              </span>
              <div>
                <h2 id="insightTitle">카테고리 인사이트</h2>
                <p>비교 결과에서 확인할 수 있는 핵심 변화만 정리했어요</p>
              </div>
            </div>
          </div>
          <div className="insight-grid">
            {insights.map((card, i) => (
              <article className="insight-card" key={i}>
                <span className="insight-number">0{i + 1}</span>
                <h3>{card.title}</h3>
                <p>{card.body}</p>
              </article>
            ))}
          </div>
          <div className="scope-note">
            <Icon name="info" small />
            <span>
              고정지출을 제외한 실제 소비 순액을 기준으로 분석했어요. 결제
              취소는 원 결제에 반영된 금액을 사용합니다.
            </span>
          </div>
        </section>
      </main>
    </div>
  );
}

export default CategoryDetailPage;
