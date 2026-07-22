const WON = new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 0 });

const INITIAL_SYNC_IN_PROGRESS = new Set([
  "NOT_STARTED",
  "FETCHING",
  "ANALYZING",
]);

const toNumber = (value) => Number(value ?? 0);

const formatWon = (value) => `${WON.format(toNumber(value))}원`;

const formatDate = (value) => {
  if (!value) return "—";
  const [, month, day] = value.split("-").map(Number);
  return `${month}월 ${day}일`;
};

const formatTime = (value) => value?.slice(11, 16) || "";

const formatDateWithWeekday = (value) => {
  if (!value) return "";
  const date = new Date(`${value}T00:00:00`);
  return `${date.getMonth() + 1}월 ${date.getDate()}일 ${["일", "월", "화", "수", "목", "금", "토"][date.getDay()]}요일`;
};

const formatShortDate = (value) => {
  if (!value) return "—";
  const [, month, day] = value.split("-").map(Number);
  return `${month}.${day}`;
};

const formatChartTick = (value) => {
  if (value === 0) return "0";
  if (value >= 10000)
    return `${WON.format(value / 10000).replace(/\.0$/, "")}만`;
  return WON.format(value);
};

const todayDate = () => {
  const now = new Date();
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  return now.toISOString().slice(0, 10);
};

const shiftDateBy = (isoDate, deltaDays) => {
  const [y, m, d] = isoDate.split("-").map(Number);
  const date = new Date(y, m - 1, d + deltaDays);
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
};

const paymentLabel = (transaction) => {
  if (transaction.paymentMethod === "CASH") return "현금";
  if (transaction.paymentMethod === "TRANSFER") return "계좌이체";
  return (
    [transaction.cardOrganizationName, transaction.cardNumberMasked]
      .filter(Boolean)
      .join(" · ") || "카드"
  );
};

const transactionAmount = (value) => {
  const amount = toNumber(value);
  return `${amount > 0 ? "-" : amount < 0 ? "+" : ""}${formatWon(Math.abs(amount))}`;
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

const errorMessage = (error, fallback) =>
  error?.response?.data?.message ||
  (error?.response?.status === 401 ? "로그인이 필요합니다." : fallback);

// 고정지출 태그 거래는 목록에는 표시하되 순사용액 집계에서는 제외한다(dayguide.md §5.1, 이중차감 방지).
const netAmountForTotal = (transaction) =>
  transaction.fixedExpenseId ? 0 : toNumber(transaction.netAmount);

export {
  INITIAL_SYNC_IN_PROGRESS,
  WON,
  errorMessage,
  formatChartTick,
  formatDate,
  formatDateWithWeekday,
  formatShortDate,
  formatTime,
  formatWon,
  netAmountForTotal,
  paymentLabel,
  shiftDateBy,
  todayDate,
  toNumber,
  transactionAmount,
  transactionVisual,
};
