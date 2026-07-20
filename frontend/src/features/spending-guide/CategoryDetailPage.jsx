import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { getCategoryAnalysis } from "../../api/spendingGuideApi";
import "./CategoryDetailPage.css";

const CATEGORY_COLORS = [
  "#22c55e",
  "#14b8a6",
  "#60a5fa",
  "#f59e0b",
  "#a78bfa",
  "#f472b6",
];
const PHASE_LABEL = { EARLY: "초반", MIDDLE: "중반", LATE: "후반" };

const toNumber = (value) => Number(value ?? 0);
const formatWon = (value) =>
  `${Math.round(toNumber(value)).toLocaleString("ko-KR")}원`;
const formatDelta = (value) => {
  const rounded = Math.round(toNumber(value));
  return `${rounded > 0 ? "+" : ""}${rounded.toLocaleString("ko-KR")}원`;
};
const formatShortDate = (value) => {
  if (!value) return "—";
  const [, month, day] = value.split("-").map(Number);
  return `${month}.${day}`;
};
const errorMessage = (error, fallback) =>
  error?.response?.data?.message ||
  (error?.response?.status === 401 ? "로그인이 필요합니다." : fallback);

function describeInsight(insight, categoryName) {
  switch (insight.type) {
    case "PERIOD_CHANGE": {
      const amount = Math.abs(toNumber(insight.amount));
      const rate = insight.rate != null ? toNumber(insight.rate) : null;
      if (insight.direction === "NEW") {
        return {
          title: "이번 주기에 새로 발생한 소비예요",
          body: `${categoryName} 소비가 이전 급여주기에는 없었는데 이번 주기에 ${formatWon(amount)} 발생했어요.`,
        };
      }
      if (insight.direction === "INCREASED") {
        return {
          title: "이전보다 소비가 늘었어요",
          body: `${categoryName} 소비가 이전 급여주기보다 ${formatWon(amount)}${rate != null ? `, 약 ${rate}%` : ""} 증가했어요.`,
        };
      }
      if (insight.direction === "DECREASED") {
        return {
          title: "이전보다 소비가 줄었어요",
          body: `${categoryName} 소비가 이전 급여주기보다 ${formatWon(amount)}${rate != null ? `, 약 ${rate}%` : ""} 감소했어요.`,
        };
      }
      return {
        title: "이전과 비슷한 수준이에요",
        body: `${categoryName} 소비가 이전 급여주기와 큰 차이가 없어요.`,
      };
    }
    case "PEAK_PHASE": {
      const label = PHASE_LABEL[insight.phase] ?? "";
      return {
        title: `${label}에 가장 많이 사용했어요`,
        body: `${categoryName} 소비의 ${toNumber(insight.percentage)}%가 급여주기 ${label} 구간에 발생했어요.`,
      };
    }
    case "CYCLE_TREND": {
      const count = insight.cycleCount ?? 0;
      if (insight.direction === "INCREASED") {
        return {
          title: "연속해서 증가하고 있어요",
          body: `${categoryName} 소비가 최근 ${count}번의 급여주기 동안 계속 늘었어요.`,
        };
      }
      if (insight.direction === "DECREASED") {
        return {
          title: "연속해서 감소하고 있어요",
          body: `${categoryName} 소비가 최근 ${count}번의 급여주기 동안 계속 줄었어요.`,
        };
      }
      if (insight.direction === "UNCHANGED") {
        return {
          title: "일정한 수준을 유지하고 있어요",
          body: `${categoryName} 소비가 최근 ${count}번의 급여주기 동안 비슷한 수준을 유지했어요.`,
        };
      }
      return {
        title: "급여주기마다 변동이 있어요",
        body: `${categoryName} 소비가 일정한 방향보다 급여주기별로 다르게 움직이고 있어요.`,
      };
    }
    default:
      return null;
  }
}

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
  const [searchParams] = useSearchParams();
  const initialBudgetId = useMemo(() => {
    const raw = searchParams.get("budgetId");
    return raw ? Number(raw) : undefined;
  }, [searchParams]);

  const [analysis, setAnalysis] = useState(null);
  const [selectedCategoryId, setSelectedCategoryId] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [pageError, setPageError] = useState("");

  const load = useCallback(async (budgetId, categoryId) => {
    setIsLoading(true);
    setPageError("");
    try {
      const response = await getCategoryAnalysis({ budgetId, categoryId });
      setAnalysis(response);
      setSelectedCategoryId(response.selectedCategory?.categoryId ?? null);
    } catch (error) {
      setPageError(
        errorMessage(error, "카테고리 소비 상세를 불러오지 못했어요."),
      );
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    const run = () => load(initialBudgetId, undefined);
    run();
  }, [load, initialBudgetId]);

  const cycle = analysis?.cycle;
  const selectedCategory = analysis?.selectedCategory;
  const categories = useMemo(() => analysis?.categories ?? [], [analysis]);

  const coloredCategories = useMemo(
    () =>
      categories.map((item, index) => ({
        ...item,
        color: CATEGORY_COLORS[index % CATEGORY_COLORS.length],
      })),
    [categories],
  );

  const maxAmount = useMemo(
    () =>
      Math.max(
        1,
        ...categories.flatMap((item) => [
          toNumber(item.selectedAmount),
          toNumber(item.previousAmount),
        ]),
      ),
    [categories],
  );

  const selectedColor =
    coloredCategories.find((item) => item.categoryId === selectedCategoryId)
      ?.color ?? CATEGORY_COLORS[0];

  const handleCycleNav = (budgetId) => {
    if (!budgetId || isLoading) return;
    load(budgetId, selectedCategoryId ?? undefined);
  };

  const handleSelectCategory = (categoryId) => {
    if (!cycle || isLoading || categoryId === selectedCategoryId) return;
    load(cycle.budgetId, categoryId);
  };

  const retry = () =>
    load(cycle?.budgetId ?? initialBudgetId, selectedCategoryId ?? undefined);

  const trend = useMemo(() => {
    const points = selectedCategory?.trend ?? [];
    if (points.length === 0) return null;
    const width = 640;
    const height = 250;
    const left = 54;
    const right = 28;
    const top = 34;
    const bottom = 44;
    const values = points.map((point) => toNumber(point.amount));
    const max = Math.max(50000, Math.ceil(Math.max(...values) / 50000) * 50000);
    const plotWidth = width - left - right;
    const plotHeight = height - top - bottom;
    const denom = Math.max(1, points.length - 1);
    const chartPoints = points.map((point, index) => ({
      x:
        points.length === 1
          ? left + plotWidth / 2
          : left + (plotWidth / denom) * index,
      y: top + ((max - toNumber(point.amount)) / max) * plotHeight,
      value: toNumber(point.amount),
      label: `${Number(point.yearMonth.split("-")[1])}월 급여주기`,
    }));
    const linePoints = chartPoints.map((p) => `${p.x},${p.y}`).join(" ");
    const areaPoints = `${left},${top + plotHeight} ${linePoints} ${left + plotWidth},${top + plotHeight}`;
    const grid = [0, 0.5, 1].map((rate) => {
      const y = top + plotHeight * rate;
      const value = max * (1 - rate);
      return { y, label: value === 0 ? "0" : `${Math.round(value / 10000)}만` };
    });
    return {
      width,
      height,
      left,
      right,
      points: chartPoints,
      linePoints,
      areaPoints,
      grid,
    };
  }, [selectedCategory]);

  const phaseEntries = selectedCategory
    ? [
        {
          key: "early",
          label: "초반 · 1~10일",
          value: selectedCategory.phase.early,
        },
        {
          key: "middle",
          label: "중반 · 11~20일",
          value: selectedCategory.phase.middle,
        },
        {
          key: "late",
          label: "후반 · 21일 이후",
          value: selectedCategory.phase.late,
        },
      ]
    : [];

  const insights = useMemo(() => {
    if (!selectedCategory) return [];
    return selectedCategory.insights
      .map((insight) => describeInsight(insight, selectedCategory.categoryName))
      .filter(Boolean);
  }, [selectedCategory]);

  const noCategorySpend = analysis?.emptyReason === "NO_CATEGORY_SPEND";

  return (
    <div className="category-detail-page" style={{ "--accent": selectedColor }}>
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
            <p>
              카테고리별 변화와 소비 시점을 비교해 지출 흐름을 확인해보세요.
            </p>
          </div>
          {cycle && (
            <div className="cycle-nav" aria-label="급여주기 선택">
              <button
                type="button"
                aria-label="이전 급여주기"
                onClick={() => handleCycleNav(cycle.previousBudgetId)}
                disabled={!cycle.previousBudgetId || isLoading}
              >
                <Icon name="chevron-left" small />
              </button>
              <div className="cycle-copy">
                <strong>
                  {Number(cycle.yearMonth.split("-")[1])}월 급여주기
                </strong>
                <span>
                  {formatShortDate(cycle.cycleStartDate)} ~{" "}
                  {cycle.status === "IN_PROGRESS"
                    ? "오늘"
                    : formatShortDate(cycle.cycleEndDate)}{" "}
                  · {cycle.status === "IN_PROGRESS" ? "진행 중" : "완료"}
                </span>
              </div>
              <button
                type="button"
                aria-label="다음 급여주기"
                onClick={() => handleCycleNav(cycle.nextBudgetId)}
                disabled={!cycle.nextBudgetId || isLoading}
              >
                <Icon name="chevron-right" small />
              </button>
            </div>
          )}
        </div>

        {isLoading && !analysis && (
          <div className="page-loading" role="status">
            <span className="spinner" />
            불러오는 중이에요...
          </div>
        )}

        {pageError && (
          <div className="page-error">
            <span>{pageError}</span>
            <button type="button" onClick={retry}>
              다시 시도
            </button>
          </div>
        )}

        {analysis && noCategorySpend && (
          <div className="page-error">
            <span>이번 급여주기에는 카테고리별 소비 내역이 아직 없어요.</span>
          </div>
        )}

        {analysis && !noCategorySpend && (
          <>
            <section className="panel" aria-labelledby="comparisonTitle">
              <div className="panel-head">
                <div className="panel-title">
                  <span className="badge-icon">
                    <Icon name="chart" />
                  </span>
                  <div>
                    <h2 id="comparisonTitle">카테고리별 변화</h2>
                    <p>
                      이전 급여주기와 비교해 소비가 어디에서 달라졌는지
                      보여드려요
                    </p>
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
                {coloredCategories.map((row) => (
                  <button
                    type="button"
                    key={row.categoryId}
                    className={`category-row${row.categoryId === selectedCategoryId ? " active" : ""}`}
                    style={{ "--accent": row.color }}
                    onClick={() => handleSelectCategory(row.categoryId)}
                  >
                    <span className="category-name">
                      <i className="category-dot" />
                      <span>
                        {row.categoryName}
                        <small>
                          전체 소비의 {toNumber(row.selectedShare)}%
                        </small>
                      </span>
                    </span>
                    <span className="bar-pair">
                      <i className="bar-track">
                        <i
                          className="bar-fill"
                          style={{
                            width: `${(toNumber(row.selectedAmount) / maxAmount) * 100}%`,
                          }}
                        />
                      </i>
                      <i className="bar-track">
                        <i
                          className="bar-fill previous"
                          style={{
                            width: `${(toNumber(row.previousAmount) / maxAmount) * 100}%`,
                          }}
                        />
                      </i>
                    </span>
                    <span className="amount-cell">
                      <strong>{formatWon(row.selectedAmount)}</strong>
                      <span>이전 {formatWon(row.previousAmount)}</span>
                    </span>
                    <span className="delta-cell">
                      {row.changeStatus === "INCREASED" && (
                        <span className="delta up">
                          ↑ {formatDelta(row.changeAmount)}
                        </span>
                      )}
                      {row.changeStatus === "DECREASED" && (
                        <span className="delta down">
                          ↓ {formatDelta(row.changeAmount)}
                        </span>
                      )}
                      {row.changeStatus === "NEW" && (
                        <span className="delta up">NEW</span>
                      )}
                      {row.changeStatus === "UNCHANGED" && (
                        <span className="delta">변동 없음</span>
                      )}
                    </span>
                  </button>
                ))}
              </div>
            </section>

            <div className="detail-tabs" aria-label="분석할 카테고리 선택">
              {coloredCategories.map((item) => (
                <button
                  type="button"
                  key={item.categoryId}
                  className={`category-tab${item.categoryId === selectedCategoryId ? " active" : ""}`}
                  style={{ "--accent": item.color }}
                  onClick={() => handleSelectCategory(item.categoryId)}
                >
                  {item.categoryName}
                </button>
              ))}
            </div>

            {selectedCategory && (
              <>
                <div className="analysis-grid">
                  <section className="panel" aria-labelledby="trendTitle">
                    <div className="panel-head">
                      <div className="panel-title">
                        <span
                          className="badge-icon"
                          style={{
                            color: "#2769c5",
                            background: "var(--blue-soft)",
                          }}
                        >
                          <Icon name="trend" />
                        </span>
                        <div>
                          <h2 id="trendTitle">급여주기별 소비 흐름</h2>
                          <p>
                            최근 급여주기에서 선택한 카테고리의 변화를 확인해요
                          </p>
                        </div>
                      </div>
                      <span
                        className="selected-category"
                        style={{
                          color: selectedColor,
                          background: `color-mix(in srgb, ${selectedColor} 12%, white)`,
                        }}
                      >
                        {selectedCategory.categoryName}
                      </span>
                    </div>
                    {trend && trend.points.length > 1 ? (
                      <svg
                        className="trend-chart"
                        viewBox="0 0 640 250"
                        role="img"
                        aria-label="선택 카테고리의 급여주기별 소비 추이"
                      >
                        <defs>
                          <linearGradient
                            id="trendGradient"
                            x1="0"
                            y1="0"
                            x2="0"
                            y2="1"
                          >
                            <stop
                              offset="0%"
                              stopColor={selectedColor}
                              stopOpacity=".20"
                            />
                            <stop
                              offset="100%"
                              stopColor={selectedColor}
                              stopOpacity=".01"
                            />
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
                        <polygon
                          className="trend-area"
                          points={trend.areaPoints}
                        />
                        <polyline
                          className="trend-line"
                          points={trend.linePoints}
                        />
                        {trend.points.map((p, i) => (
                          <g key={i}>
                            <circle
                              className="trend-point"
                              cx={p.x}
                              cy={p.y}
                              r="5"
                            />
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
                              {p.label}
                            </text>
                          </g>
                        ))}
                      </svg>
                    ) : (
                      <p className="empty-note">
                        데이터가 더 쌓이면 급여주기별 추이를 확인할 수 있어요.
                      </p>
                    )}
                  </section>

                  <section className="panel" aria-labelledby="timingTitle">
                    <div className="panel-head">
                      <div className="panel-title">
                        <span
                          className="badge-icon"
                          style={{
                            color: "#734bd1",
                            background: "var(--purple-soft)",
                          }}
                        >
                          <Icon name="calendar" />
                        </span>
                        <div>
                          <h2 id="timingTitle">급여주기 내 소비 시점</h2>
                          <p>
                            선택한 카테고리의 소비가 언제 집중됐는지 보여드려요
                          </p>
                        </div>
                      </div>
                    </div>
                    <div className="timing-stack">
                      <div>
                        <div className="subsection-title">
                          <strong>
                            {selectedCategory.categoryName} 소비 시점
                          </strong>
                          <span>주기 진행 구간 기준</span>
                        </div>
                        <div
                          className="phase-bar"
                          aria-label="급여주기 구간별 소비 비중"
                        >
                          <span
                            className="early"
                            style={{
                              width: `${toNumber(selectedCategory.phase.early.percentage)}%`,
                            }}
                          />
                          <span
                            className="middle"
                            style={{
                              width: `${toNumber(selectedCategory.phase.middle.percentage)}%`,
                            }}
                          />
                          <span
                            className="late"
                            style={{
                              width: `${toNumber(selectedCategory.phase.late.percentage)}%`,
                            }}
                          />
                        </div>
                        <div className="phase-list">
                          {phaseEntries.map((entry) => (
                            <div className="phase-item" key={entry.key}>
                              <span>{entry.label}</span>
                              <strong>
                                {toNumber(entry.value.percentage)}%
                              </strong>
                            </div>
                          ))}
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
                              style={{
                                width: `${toNumber(selectedCategory.dayType.weekday.percentage)}%`,
                              }}
                            />
                          </div>
                          <strong>
                            {formatWon(selectedCategory.dayType.weekday.amount)}
                          </strong>
                        </div>
                        <div className="weekday-row">
                          <span>주말</span>
                          <div className="weekday-track">
                            <div
                              className="weekday-fill weekend"
                              style={{
                                width: `${toNumber(selectedCategory.dayType.weekend.percentage)}%`,
                              }}
                            />
                          </div>
                          <strong>
                            {formatWon(selectedCategory.dayType.weekend.amount)}
                          </strong>
                        </div>
                      </div>
                    </div>
                  </section>
                </div>

                <section
                  className="panel insight-panel"
                  aria-labelledby="insightTitle"
                >
                  <div className="panel-head">
                    <div className="panel-title">
                      <span
                        className="badge-icon"
                        style={{
                          color: "#a96700",
                          background: "var(--orange-soft)",
                        }}
                      >
                        <Icon name="sparkle" />
                      </span>
                      <div>
                        <h2 id="insightTitle">카테고리 인사이트</h2>
                        <p>
                          비교 결과에서 확인할 수 있는 핵심 변화만 정리했어요
                        </p>
                      </div>
                    </div>
                  </div>
                  {insights.length > 0 ? (
                    <div className="insight-grid">
                      {insights.map((card, i) => (
                        <article className="insight-card" key={i}>
                          <span className="insight-number">0{i + 1}</span>
                          <h3>{card.title}</h3>
                          <p>{card.body}</p>
                        </article>
                      ))}
                    </div>
                  ) : (
                    <p className="empty-note">
                      아직 인사이트를 만들 만큼의 데이터가 쌓이지 않았어요.
                    </p>
                  )}
                  <div className="scope-note">
                    <Icon name="info" small />
                    <span>
                      고정지출을 제외한 실제 소비 순액을 기준으로 분석했어요.
                      결제 취소는 원 결제에 반영된 금액을 사용합니다.
                    </span>
                  </div>
                </section>
              </>
            )}
          </>
        )}
      </main>
    </div>
  );
}

export default CategoryDetailPage;
