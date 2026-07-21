import {
  useCallback,
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
  createManualTransaction,
  getCardConnections,
  getCategories,
  getConsumptionHistorySummary,
  getConsumptionHistoryTransactions,
  getSpendingGuideSummary,
  getSpendingTransactions,
  syncCardTransactions,
} from "../../api/spendingGuideApi";
import { shiftDateBy, todayDate } from "./spendingGuideUtils";
import "./SpendingHistoryPage.css";

const ICONS = {
  receipt: (
    <>
      <path d="M6 3h12v18l-3-2-3 2-3-2-3 2V3Z" />
      <path d="M9 8h6m-6 4h6" />
    </>
  ),
  "chevron-left": <path d="m15 18-6-6 6-6" />,
  "chevron-right": <path d="m9 18 6-6-6-6" />,
  "arrow-left": <path d="M19 12H5m6-6-6 6 6 6" />,
  plus: <path d="M12 5v14M5 12h14" />,
  repeat: (
    <>
      <path d="m17 2 4 4-4 4" />
      <path d="M3 11V9a3 3 0 0 1 3-3h18M7 22l-4-4 4-4" />
      <path d="M21 13v2a3 3 0 0 1-3 3H3" />
    </>
  ),
  "trend-down": (
    <>
      <path d="m4 7 6 6 4-4 6 6" />
      <path d="M15 15h5v-5" />
    </>
  ),
  coffee: (
    <>
      <path d="M5 8h11v6a5 5 0 0 1-5 5H10a5 5 0 0 1-5-5V8Z" />
      <path d="M16 10h2a3 3 0 0 1 0 6h-2M8 4v2m4-2v2" />
    </>
  ),
  bag: (
    <>
      <path d="M5 8h14l1 13H4L5 8Z" />
      <path d="M9 8V6a3 3 0 0 1 6 0v2" />
    </>
  ),
  utensils: (
    <>
      <path d="M7 3v8m-3-8v5a3 3 0 0 0 6 0V3m-3 8v10M17 3v18m0-18c2 1 3 3 3 6v3h-3" />
    </>
  ),
  card: (
    <>
      <rect x="3" y="5" width="18" height="14" rx="2" />
      <path d="M3 10h18M7 15h4" />
    </>
  ),
  car: (
    <>
      <path d="m5 17-2-2v-4l2-1 2-5h10l2 5 2 1v4l-2 2H5Z" />
      <path d="M7 14h.01M17 14h.01M6 17v2m12-2v2" />
    </>
  ),
};

const PAYMENT_METHOD_LABELS = {
  CASH: "현금",
  TRANSFER: "계좌이체",
  CARD: "카드",
};

const WON = new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 0 });
const toNumber = (value) => Number(value ?? 0);
const formatWon = (value) => `${WON.format(toNumber(value))}원`;
const errorMessage = (error, fallback) =>
  error?.response?.data?.message ||
  (error?.response?.status === 401 ? "로그인이 필요합니다." : fallback);

const dayOfMonth = (isoDate) => Number(isoDate.split("-")[2]);
const formatMonthDay = (isoDate) => {
  const [, m, d] = isoDate.split("-");
  return `${Number(m)}.${Number(d)}`;
};
const formatMonthDayKo = (isoDate) => {
  const [, m, d] = isoDate.split("-");
  return `${Number(m)}월 ${Number(d)}일`;
};

const transactionVisual = (transaction) => {
  const category = transaction.categoryName || "";
  if (/카페|간식/.test(category)) return { icon: "coffee", tone: "" };
  if (/식비|배달|외식/.test(category))
    return { icon: "utensils", tone: "orange" };
  if (/교통|택시|주유|차량/.test(category))
    return { icon: "car", tone: "blue" };
  if (/편의점|마트|쇼핑/.test(category)) return { icon: "bag", tone: "orange" };
  if (toNumber(transaction.netAmount) < 0) return { icon: "card", tone: "red" };
  return {
    icon: transaction.paymentMethod === "CARD" ? "card" : "receipt",
    tone: "",
  };
};

const nowLocalInputValue = () => {
  const now = new Date();
  const pad = (n) => String(n).padStart(2, "0");
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`;
};

function HistoryIcon({ name, small = false, spin = false }) {
  return (
    <svg
      className={`icon${small ? " icon-sm" : ""}${spin ? " icon-spin" : ""}`}
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
      {ICONS[name]}
    </svg>
  );
}

function DailyChart({ daily, isCurrentCycle }) {
  const chartRef = useRef(null);
  const [size, setSize] = useState({ width: 700, height: 190 });

  useLayoutEffect(() => {
    const chart = chartRef.current;
    if (!chart) return undefined;
    const updateSize = () =>
      setSize({
        width: Math.max(chart.clientWidth, 320),
        height: Math.max(chart.clientHeight, 160),
      });
    updateSize();
    const observer = new ResizeObserver(updateSize);
    observer.observe(chart);
    return () => observer.disconnect();
  }, []);

  const values = daily.map((item) => item[1]);
  const peak = Math.max(...values);
  const peakIndex = values.indexOf(peak);
  const scaleMax = Math.max(30, Math.ceil(peak / 30) * 30);
  const x = (index) =>
    daily.length === 1
      ? size.width / 2
      : (index / (daily.length - 1)) * size.width;
  const y = (value) => 8 + (size.height - 10) * (1 - value / scaleMax);
  const points = daily.map(
    (item, index) => `${x(index).toFixed(1)},${y(item[1]).toFixed(1)}`,
  );
  const linePath = `M ${points.join(" L ")}`;
  const areaPath = `${linePath} L ${x(daily.length - 1).toFixed(1)},${size.height} L ${x(0).toFixed(1)},${size.height} Z`;
  const gridLevels = [scaleMax, (scaleMax * 2) / 3, scaleMax / 3, 0];
  const labelStep = Math.max(1, Math.ceil((daily.length - 1) / 6));
  const labelIndexes = [];
  for (let index = 0; index < daily.length; index += labelStep)
    labelIndexes.push(index);
  if (labelIndexes.at(-1) !== daily.length - 1)
    labelIndexes.push(daily.length - 1);

  return (
    <div
      className="chart-area"
      role="img"
      aria-label={`${daily.length}일간 일별 순사용액 선 그래프. 최고 지출 ${daily[peakIndex][0]} ${(peak * 1000).toLocaleString("ko-KR")}원`}
    >
      <div className="chart-grid">
        {gridLevels.map((value, index) => (
          <div
            className="grid-line"
            key={value}
            style={{ top: `${(index / (gridLevels.length - 1)) * 100}%` }}
          >
            <span>{value ? `${Number((value / 10).toFixed(1))}만` : "0"}</span>
          </div>
        ))}
      </div>
      <div className="daily-line-chart" ref={chartRef}>
        <svg viewBox={`0 0 ${size.width} ${size.height}`} aria-hidden="true">
          <defs>
            <linearGradient
              id="consumeLineAreaGradient"
              x1="0"
              y1="0"
              x2="0"
              y2="1"
            >
              <stop offset="0%" stopColor="#25c96a" stopOpacity=".22" />
              <stop offset="100%" stopColor="#25c96a" stopOpacity=".02" />
            </linearGradient>
          </defs>
          <path className="line-area" d={areaPath} />
          <path className="line-path" d={linePath} />
          {daily.map((item, index) => (
            <g key={`${item[0]}-${index}`}>
              <circle
                className={`line-point${index === peakIndex || (isCurrentCycle && index === daily.length - 1) ? " highlight" : ""}`}
                cx={x(index)}
                cy={y(item[1])}
                r="3"
              />
              <circle className="line-hit" cx={x(index)} cy={y(item[1])} r="9">
                <title>
                  {item[0]} · {(item[1] * 1000).toLocaleString("ko-KR")}원
                </title>
              </circle>
            </g>
          ))}
          <text
            className="peak-label"
            x={x(peakIndex)}
            y={Math.max(11, y(peak) - 10)}
          >
            최고 {Number((peak / 10).toFixed(1))}만원
          </text>
        </svg>
      </div>
      <div className="daily-axis-labels" aria-hidden="true">
        {labelIndexes.map((index, labelIndex) => {
          const position =
            daily.length === 1 ? 50 : (index / (daily.length - 1)) * 100;
          const transform =
            labelIndex === 0
              ? "none"
              : index === daily.length - 1
                ? "translateX(-100%)"
                : "translateX(-50%)";
          return (
            <span
              className="day-label"
              key={index}
              style={{ left: `${position}%`, transform }}
            >
              {daily[index][0]}
            </span>
          );
        })}
      </div>
    </div>
  );
}

function groupTransactions(items) {
  const groups = [];
  items.forEach((item) => {
    const last = groups.at(-1);
    if (last && last.date === item.usedDate) {
      last.items.push(item);
      last.total += toNumber(item.netAmount);
    } else {
      groups.push({
        date: item.usedDate,
        items: [item],
        total: toNumber(item.netAmount),
      });
    }
  });
  return groups;
}

function TransactionGroup({ group, isToday }) {
  return (
    <div className="history-group">
      <div className="history-date">
        <strong>{`${formatMonthDayKo(group.date)}${isToday ? " · 오늘" : ""}`}</strong>
        <span>{`순사용 ${formatWon(group.total)}`}</span>
      </div>
      <div className="transaction-list">
        {group.items.map((item) => {
          const visual = transactionVisual(item);
          const isManual = item.source === "MANUAL";
          const badge =
            item.status === "PARTIAL_CANCELED"
              ? "일부 취소됨"
              : item.status === "CANCELED"
                ? "취소"
                : isManual
                  ? "직접 입력"
                  : "";
          const isRefund = toNumber(item.netAmount) < 0;
          const amountSub =
            item.status === "PARTIAL_CANCELED" && item.originalAmount != null
              ? `원 결제 ${formatWon(item.originalAmount)}`
              : item.status === "CANCELED"
                ? "취소행"
                : "";
          const sourceLabel = isManual
            ? `${PAYMENT_METHOD_LABELS[item.paymentMethod] || item.paymentMethod} · 직접 입력`
            : `${item.cardOrganizationName || ""} ${item.cardNumberMasked || ""} · 카드 자동수집`.trim();
          return (
            <div className="transaction-row" key={item.id}>
              <span className={`tx-icon ${visual.tone}`}>
                <HistoryIcon name={visual.icon} />
              </span>
              <span className="tx-info">
                <span className="tx-name">
                  <strong>{item.merchantDisplayName}</strong>
                  {badge && (
                    <em
                      className={`tiny-badge${badge.includes("취소") ? " cancel" : ""}`}
                    >
                      {badge}
                    </em>
                  )}
                </span>
                <span className="tx-meta">{`${item.usedAt.slice(11, 16)} · ${item.categoryName}`}</span>
                <span className="tx-meta">{sourceLabel}</span>
              </span>
              <span className={`tx-amount${isRefund ? " refund" : ""}`}>
                <strong>{`${isRefund ? "+" : "-"}${formatWon(Math.abs(toNumber(item.netAmount)))}`}</strong>
                <span>{amountSub}</span>
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function SpendingHistoryPage() {
  const [cycleBudgetId, setCycleBudgetId] = useState(null);
  const [summary, setSummary] = useState(null);
  const [transactionsData, setTransactionsData] = useState(null);
  const [categories, setCategories] = useState([]);
  const [categoriesError, setCategoriesError] = useState("");
  const [allowCard, setAllowCard] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isTransactionsLoading, setIsTransactionsLoading] = useState(false);
  const [pageError, setPageError] = useState("");
  const [transactionsError, setTransactionsError] = useState("");
  const [formError, setFormError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSyncing, setIsSyncing] = useState(false);
  const [syncMessage, setSyncMessage] = useState("");
  const [toast, setToast] = useState("");
  const transactionsPanelRef = useRef(null);
  const initialDateTime = useMemo(() => nowLocalInputValue(), []);
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedDate = searchParams.get("date");
  const [initialBudgetId] = useState(() => searchParams.get("budgetId"));
  const [dayItems, setDayItems] = useState(null);
  const [isDayLoading, setIsDayLoading] = useState(false);
  const [dayError, setDayError] = useState("");
  const [dailyGuideAmount, setDailyGuideAmount] = useState(null);
  const today = todayDate();

  useEffect(() => {
    if (!toast) return undefined;
    const timer = window.setTimeout(() => setToast(""), 2200);
    return () => window.clearTimeout(timer);
  }, [toast]);

  useEffect(() => {
    getSpendingGuideSummary()
      .then((data) =>
        setDailyGuideAmount(toNumber(data.dailyRecommendedAmount)),
      )
      .catch(() => {});
  }, []);

  const loadDay = useCallback((dateValue) => {
    setIsDayLoading(true);
    setDayError("");
    getSpendingTransactions({ date: dateValue, size: 50 })
      .then((result) => setDayItems(result.items ?? []))
      .catch((error) =>
        setDayError(
          errorMessage(error, "해당 날짜의 내역을 불러오지 못했어요."),
        ),
      )
      .finally(() => setIsDayLoading(false));
  }, []);

  useEffect(() => {
    const run = () => {
      if (selectedDate) loadDay(selectedDate);
    };
    run();
  }, [selectedDate, loadDay]);

  const goToDate = (nextDate) => {
    const next = new URLSearchParams(searchParams);
    if (nextDate) next.set("date", nextDate);
    else next.delete("date");
    setSearchParams(next);
  };

  const dayNetTotal = useMemo(
    () =>
      (dayItems ?? []).reduce((sum, item) => sum + toNumber(item.netAmount), 0),
    [dayItems],
  );
  const dayGroups = useMemo(
    () => groupTransactions(dayItems ?? []),
    [dayItems],
  );

  const fetchTransactionsPage = useCallback(
    (budgetId, page) =>
      getConsumptionHistoryTransactions({ budgetId, page, size: 10 }),
    [],
  );

  const loadTransactions = useCallback(
    async (budgetId, page) => {
      setIsTransactionsLoading(true);
      setTransactionsError("");
      try {
        const data = await fetchTransactionsPage(budgetId, page);
        setTransactionsData(data);
      } catch (error) {
        setTransactionsError(
          errorMessage(error, "결제내역을 불러오지 못했어요."),
        );
      } finally {
        setIsTransactionsLoading(false);
      }
    },
    [fetchTransactionsPage],
  );

  const loadCycle = useCallback(
    async (targetBudgetId) => {
      setIsLoading(true);
      setPageError("");
      setIsTransactionsLoading(true);
      setTransactionsError("");
      try {
        const summaryData = await getConsumptionHistorySummary(
          targetBudgetId ?? undefined,
        );
        setSummary(summaryData);
        setCycleBudgetId(summaryData.cycle.budgetId);
        const transactionsPage = await fetchTransactionsPage(
          summaryData.cycle.budgetId,
          0,
        );
        setTransactionsData(transactionsPage);
      } catch (error) {
        setPageError(errorMessage(error, "소비내역을 불러오지 못했어요."));
      } finally {
        setIsLoading(false);
        setIsTransactionsLoading(false);
      }
    },
    [fetchTransactionsPage],
  );

  useEffect(() => {
    const run = () =>
      loadCycle(initialBudgetId ? Number(initialBudgetId) : null);
    run();
  }, [loadCycle, initialBudgetId]);

  useEffect(() => {
    getCategories()
      .then(setCategories)
      .catch((error) =>
        setCategoriesError(
          errorMessage(error, "카테고리를 불러오지 못했어요."),
        ),
      );
    getCardConnections()
      .then((connections) =>
        setAllowCard(
          Boolean(
            !connections?.connections?.length || !connections?.cardSyncEnabled,
          ),
        ),
      )
      .catch(() => setAllowCard(false));
  }, []);

  const moveTransactionPage = (direction) => {
    if (!transactionsData) return;
    const nextPage = transactionsData.page + direction;
    if (nextPage < 0 || nextPage > transactionsData.totalPages - 1) return;
    loadTransactions(cycleBudgetId, nextPage);
    window.requestAnimationFrame(() =>
      transactionsPanelRef.current?.scrollIntoView({
        behavior: "smooth",
        block: "start",
      }),
    );
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    const usedDateTime = data.get("usedDateTime");
    const [usedDate, usedTime] = usedDateTime.split("T");
    const payload = {
      paymentMethod: data.get("paymentMethod"),
      usedDate,
      usedTime: usedTime ? `${usedTime}:00` : null,
      amount: Number(data.get("amount")),
      categoryId: Number(data.get("categoryId")),
      merchantName: data.get("merchantName"),
      memo: data.get("memo") || undefined,
    };
    setFormError("");
    setIsSubmitting(true);
    try {
      await createManualTransaction(payload);
      form.reset();
      setToast("결제내역을 저장했어요.");
      if (cycleBudgetId) await loadCycle(cycleBudgetId);
    } catch (error) {
      setFormError(errorMessage(error, "결제내역을 저장하지 못했어요."));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSync = async () => {
    setIsSyncing(true);
    try {
      const result = await syncCardTransactions();
      if (result?.locked) {
        setToast("카드내역 동기화가 이미 진행 중이에요.");
      } else {
        setSyncMessage("방금 동기화했어요.");
        setToast("결제내역을 동기화했어요.");
        if (cycleBudgetId) await loadCycle(cycleBudgetId);
      }
    } catch (error) {
      setToast(errorMessage(error, "결제내역을 동기화하지 못했어요."));
    } finally {
      setIsSyncing(false);
    }
  };

  const cycle = summary?.cycle;
  const comparison = summary?.comparison;
  const daily = useMemo(
    () =>
      (summary?.dailyTrend ?? []).map((item) => [
        `${dayOfMonth(item.date)}일`,
        toNumber(item.netAmount) / 1000,
      ]),
    [summary],
  );
  const groups = useMemo(
    () => groupTransactions(transactionsData?.items ?? []),
    [transactionsData],
  );

  return (
    <div className="consume-history-page">
      <main className="page">
        <Link className="back-link" to="/dashboard/spending">
          <HistoryIcon name="arrow-left" />
          소비가이드로 돌아가기
        </Link>

        <div className="page-head">
          <div>
            <h1>전체 소비내역</h1>
            <p>급여 주기별 결제 흐름과 자주 이용한 곳을 함께 확인해보세요.</p>
          </div>
        </div>

        {isLoading && !summary && (
          <div className="page-loading" role="status">
            <span className="spinner" />
            불러오는 중이에요...
          </div>
        )}

        {pageError && (
          <div className="page-error">
            <span>{pageError}</span>
            <button type="button" onClick={() => loadCycle(cycleBudgetId)}>
              다시 시도
            </button>
          </div>
        )}

        {selectedDate && (
          <>
            <section className="cycle-toolbar" aria-label="조회 날짜">
              <div className="period-nav">
                <button
                  className="cycle-arrow"
                  type="button"
                  aria-label="전날"
                  onClick={() => goToDate(shiftDateBy(selectedDate, -1))}
                >
                  <HistoryIcon name="chevron-left" small />
                </button>
                <strong>{`${formatMonthDayKo(selectedDate)}${selectedDate === today ? " · 오늘" : ""}`}</strong>
                <button
                  className="cycle-arrow"
                  type="button"
                  aria-label="다음날"
                  onClick={() => goToDate(shiftDateBy(selectedDate, 1))}
                  disabled={selectedDate >= today}
                >
                  <HistoryIcon name="chevron-right" small />
                </button>
              </div>
              <div className="day-toolbar-actions">
                <input
                  type="date"
                  className="day-date-input"
                  value={selectedDate}
                  max={today}
                  onChange={(event) =>
                    event.target.value && goToDate(event.target.value)
                  }
                  aria-label="날짜 선택"
                />
                <button
                  className="ghost-button"
                  type="button"
                  onClick={() => goToDate(null)}
                >
                  전체 소비내역 보기
                </button>
              </div>
            </section>

            <section
              className="day-stats-grid"
              aria-label="선택한 날짜 소비 요약"
            >
              <article className="stat-card">
                <div className="stat-head">
                  <span className="stat-label">
                    <span className="stat-icon">
                      <HistoryIcon name="receipt" />
                    </span>
                    이 날 순사용액
                  </span>
                </div>
                <strong className="stat-value">{formatWon(dayNetTotal)}</strong>
                <span className="stat-sub">고정지출 제외</span>
              </article>
              <article className="stat-card">
                <div className="stat-head">
                  <span className="stat-label">
                    <span className="stat-icon">
                      <HistoryIcon name="trend-down" />
                    </span>
                    하루 권장액 대비
                  </span>
                </div>
                {dailyGuideAmount != null ? (
                  <>
                    <strong className="stat-value">
                      {formatWon(dailyGuideAmount)}
                    </strong>
                    <span className="stat-sub">
                      {dayNetTotal > dailyGuideAmount
                        ? `권장액보다 ${formatWon(dayNetTotal - dailyGuideAmount)} 더 썼어요`
                        : `권장액보다 ${formatWon(dailyGuideAmount - dayNetTotal)} 덜 썼어요`}
                    </span>
                  </>
                ) : (
                  <p className="empty-note">불러오는 중이에요...</p>
                )}
                <span className="stat-refund-note">
                  현재 기준 하루 권장액이에요. 지난 주기에는 금액이 달랐을 수
                  있어요.
                </span>
              </article>
            </section>

            <section
              className="panel transactions-panel"
              aria-label="선택한 날짜 결제내역"
            >
              <div className="transactions-head">
                <div className="panel-head">
                  <div>
                    <h2>{formatMonthDayKo(selectedDate)} 결제내역</h2>
                    <p>이 날짜에 기록된 결제내역이에요.</p>
                  </div>
                </div>
              </div>
              {isDayLoading && (
                <p className="empty-note">불러오는 중이에요...</p>
              )}
              {!isDayLoading && dayError && (
                <div className="page-error">
                  <span>{dayError}</span>
                  <button type="button" onClick={() => loadDay(selectedDate)}>
                    다시 시도
                  </button>
                </div>
              )}
              {!isDayLoading && !dayError && dayGroups.length === 0 && (
                <p className="empty-note">이 날짜에는 결제내역이 없어요.</p>
              )}
              {!isDayLoading &&
                !dayError &&
                dayGroups.map((group) => (
                  <TransactionGroup
                    key={group.date}
                    group={group}
                    isToday={group.date === today}
                  />
                ))}
            </section>
          </>
        )}

        {!selectedDate && cycle && (
          <>
            <section className="cycle-toolbar" aria-label="조회 급여 주기">
              <div className="period-nav">
                <button
                  className="cycle-arrow"
                  type="button"
                  aria-label="이전 급여 주기"
                  onClick={() => loadCycle(cycle.previousBudgetId)}
                  disabled={!cycle.previousBudgetId || isLoading}
                >
                  <HistoryIcon name="chevron-left" small />
                </button>
                <strong>{`${Number(cycle.yearMonth.split("-")[1])}월 급여주기 · ${formatMonthDay(cycle.cycleStartDate)} ~ ${formatMonthDay(cycle.cycleEndDate)}`}</strong>
                <button
                  className="cycle-arrow"
                  type="button"
                  aria-label="다음 급여 주기"
                  onClick={() => loadCycle(cycle.nextBudgetId)}
                  disabled={!cycle.nextBudgetId || isLoading}
                >
                  <HistoryIcon name="chevron-right" small />
                </button>
              </div>
              <span className="period-note">
                {cycle.status === "IN_PROGRESS"
                  ? `${formatMonthDayKo(cycle.dataEndDate)} 기준 · 진행 중인 주기`
                  : "완료된 급여 주기"}
              </span>
            </section>

            <section
              className="analytics-grid"
              aria-label="급여 주기 소비 요약"
            >
              <article className="stat-card">
                <div className="stat-head">
                  <span className="stat-label">
                    <span className="stat-icon">
                      <HistoryIcon name="receipt" />
                    </span>
                    이번 주기 순사용액
                  </span>
                </div>
                <strong className="stat-value">
                  {formatWon(summary.cycleNetAmount)}
                </strong>
                <span className="stat-sub">
                  {cycle.status === "IN_PROGRESS"
                    ? `고정지출 제외 · ${formatMonthDayKo(cycle.cycleStartDate)} ~ 오늘`
                    : "고정지출 제외 · 완료된 급여 주기"}
                </span>
                {toNumber(summary.canceledAmount) > 0 && (
                  <span className="stat-refund-note">{`취소·환불 ${formatWon(summary.canceledAmount)} 반영`}</span>
                )}
              </article>

              <article className="stat-card">
                {comparison ? (
                  <>
                    <div className="stat-head">
                      <span className="stat-label">
                        <span className="stat-icon">
                          <HistoryIcon name="trend-down" />
                        </span>
                        지난 주기 대비 증감
                      </span>
                      <span className="delta-badge">
                        {comparison.direction === "NEW"
                          ? "신규 지출"
                          : comparison.direction === "UNCHANGED"
                            ? "변동 없음"
                            : `${toNumber(comparison.changeRate)}% ${comparison.direction === "INCREASED" ? "증가" : "감소"}`}
                      </span>
                    </div>
                    <div className="stat-value delta-value">
                      <strong>{`${toNumber(comparison.changeAmount) >= 0 ? "+" : "-"}${formatWon(Math.abs(toNumber(comparison.changeAmount)))}`}</strong>
                    </div>
                    <span className="stat-sub">
                      {comparison.basis === "FULL_CYCLE"
                        ? "이전 급여 주기 전체와 비교"
                        : "이전 급여 주기 동일 경과일 기준"}
                    </span>
                  </>
                ) : (
                  <>
                    <div className="stat-head">
                      <span className="stat-label">
                        <span className="stat-icon">
                          <HistoryIcon name="trend-down" />
                        </span>
                        지난 주기 대비 증감
                      </span>
                    </div>
                    <p className="empty-note">
                      비교할 이전 급여 주기가 없어요.
                    </p>
                  </>
                )}
              </article>

              <article className="panel merchant-panel">
                <div className="panel-head">
                  <div>
                    <h2>많이 쓴 곳 TOP 5</h2>
                    <p>고정지출을 제외한 순사용액 기준이에요.</p>
                  </div>
                  <span className="panel-badge">이번 주기</span>
                </div>
                {summary.merchantTop5.length ? (
                  <div className="merchant-list">
                    {summary.merchantTop5.map((merchant, index) => (
                      <div className="merchant-row" key={merchant.merchantKey}>
                        <span className="merchant-rank">{index + 1}</span>
                        <span className="merchant-info">
                          <strong>{merchant.displayName}</strong>
                          <span>{`${merchant.transactionCount}회 이용`}</span>
                        </span>
                        <span className="merchant-amount">
                          {formatWon(merchant.netAmount)}
                        </span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="empty-note">아직 이용 내역이 없어요.</p>
                )}
              </article>

              <article className="panel trend-panel">
                <div className="panel-head">
                  <div>
                    <h2>일별 소비 추이</h2>
                    <p>
                      급여 주기의 모든 날짜를 기준으로 일별 순사용액을
                      보여드려요.
                    </p>
                  </div>
                  <div className="chart-meta">
                    <span className="legend-item">
                      <i className="legend-dot" />
                      순사용액
                    </span>
                    <span>단위 · 만원</span>
                  </div>
                </div>
                {daily.length > 0 && (
                  <DailyChart
                    daily={daily}
                    isCurrentCycle={cycle.status === "IN_PROGRESS"}
                  />
                )}
              </article>
            </section>

            <div className="history-layout">
              <section
                className={`panel transactions-panel${isTransactionsLoading ? " is-loading" : ""}`}
                aria-labelledby="transactionsTitle"
                ref={transactionsPanelRef}
              >
                <div className="transactions-head">
                  <div className="panel-head">
                    <div>
                      <h2 id="transactionsTitle">결제내역</h2>
                      <p>결제일 기준으로 최신 내역부터 보여드려요.</p>
                    </div>
                  </div>
                </div>

                {transactionsError && (
                  <div className="page-error">
                    <span>{transactionsError}</span>
                    <button
                      type="button"
                      onClick={() =>
                        loadTransactions(
                          cycleBudgetId,
                          transactionsData?.page ?? 0,
                        )
                      }
                    >
                      다시 시도
                    </button>
                  </div>
                )}

                {!transactionsError && groups.length === 0 && (
                  <p className="empty-note">표시할 결제내역이 없어요.</p>
                )}

                {!transactionsError && groups.length > 0 && (
                  <div>
                    {groups.map((group) => (
                      <TransactionGroup
                        key={group.date}
                        group={group}
                        isToday={
                          cycle.status === "IN_PROGRESS" &&
                          group.date === cycle.dataEndDate
                        }
                      />
                    ))}
                  </div>
                )}

                <div className="list-footer">
                  <div
                    className="pagination-controls"
                    aria-label="결제내역 페이지 이동"
                  >
                    <button
                      className="ghost-button"
                      type="button"
                      onClick={() => moveTransactionPage(1)}
                      disabled={
                        !transactionsData ||
                        transactionsData.page >=
                          transactionsData.totalPages - 1 ||
                        isTransactionsLoading
                      }
                    >
                      <HistoryIcon name="chevron-left" small />
                      이전 10건
                    </button>
                    <span className="page-progress" aria-live="polite">
                      {transactionsData
                        ? `${transactionsData.page + 1} / ${Math.max(1, transactionsData.totalPages)}페이지 · 총 ${transactionsData.totalElements}건`
                        : ""}
                    </span>
                    <button
                      className="ghost-button"
                      type="button"
                      onClick={() => moveTransactionPage(-1)}
                      disabled={
                        !transactionsData ||
                        transactionsData.page <= 0 ||
                        isTransactionsLoading
                      }
                    >
                      최신 10건
                      <HistoryIcon name="chevron-right" small />
                    </button>
                  </div>
                </div>
              </section>

              <aside
                className="panel entry-panel"
                aria-labelledby="entryPanelTitle"
              >
                <div className="entry-head">
                  <span className="stat-icon">
                    <HistoryIcon name="plus" />
                  </span>
                  <div>
                    <h2 id="entryPanelTitle">결제내역 추가</h2>
                    <p>현금과 계좌이체 내역을 바로 기록해보세요.</p>
                  </div>
                </div>
                <form onSubmit={handleSubmit}>
                  <div className="form-grid">
                    <div className="field">
                      <label htmlFor="entryAmount">금액 *</label>
                      <input
                        id="entryAmount"
                        name="amount"
                        type="number"
                        min="1"
                        step="1"
                        inputMode="numeric"
                        placeholder="0원"
                        required
                      />
                    </div>
                    <div className="field">
                      <label htmlFor="entryMethod">결제수단 *</label>
                      <select
                        id="entryMethod"
                        name="paymentMethod"
                        defaultValue=""
                        required
                      >
                        <option value="">선택</option>
                        <option value="CASH">현금</option>
                        <option value="TRANSFER">계좌이체</option>
                        {allowCard && <option value="CARD">카드</option>}
                      </select>
                    </div>
                    <div className="field">
                      <label htmlFor="entryName">사용처/내용 *</label>
                      <input
                        id="entryName"
                        name="merchantName"
                        placeholder="예: 점심 식사"
                        required
                      />
                    </div>
                    <div className="field">
                      <label htmlFor="entryDate">사용일시 *</label>
                      <input
                        id="entryDate"
                        name="usedDateTime"
                        type="datetime-local"
                        defaultValue={initialDateTime}
                        max={initialDateTime}
                        required
                      />
                    </div>
                    <div className="field">
                      <label htmlFor="entryCategory">카테고리 *</label>
                      <select
                        id="entryCategory"
                        name="categoryId"
                        defaultValue=""
                        required
                        disabled={!categories.length}
                      >
                        <option value="">선택</option>
                        {categories.map((category) => (
                          <option key={category.id} value={category.id}>
                            {category.name}
                          </option>
                        ))}
                      </select>
                      {categoriesError && (
                        <span className="form-error">{categoriesError}</span>
                      )}
                    </div>
                    <div className="field">
                      <label htmlFor="entryMemo">메모</label>
                      <textarea
                        id="entryMemo"
                        name="memo"
                        placeholder="기억해둘 내용을 적어주세요"
                      />
                    </div>
                  </div>
                  <p className="form-note">
                    카드 결제는 자동으로 불러오므로 중복 입력에 유의해주세요.
                    반복되는 결제는 고정지출 메뉴에서 관리할 수 있어요.
                  </p>
                  {formError && <p className="form-error">{formError}</p>}
                  <div className="form-actions">
                    <button
                      className="submit-button"
                      type="submit"
                      disabled={isSubmitting}
                    >
                      {isSubmitting ? "저장 중..." : "결제내역 저장"}
                    </button>
                    <button
                      className="sync-button"
                      type="button"
                      onClick={handleSync}
                      disabled={isSyncing}
                    >
                      <HistoryIcon name="repeat" small spin={isSyncing} />
                      {isSyncing ? "동기화 중..." : "결제내역 동기화"}
                    </button>
                  </div>
                  {syncMessage && <p className="sync-status">{syncMessage}</p>}
                </form>
              </aside>
            </div>
          </>
        )}
      </main>
      <div
        className={`toast${toast ? " show" : ""}`}
        role="status"
        aria-live="polite"
      >
        {toast}
      </div>
    </div>
  );
}

export default SpendingHistoryPage;
