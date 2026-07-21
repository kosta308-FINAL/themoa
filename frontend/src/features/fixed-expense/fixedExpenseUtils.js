const WON = new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 0 });

const toNumber = (value) => Number(value ?? 0);

const formatWon = (value) => `${WON.format(toNumber(value))}원`;

const formatAmount = (amount, currency) => {
  if (currency && currency !== "KRW") {
    const foreign = new Intl.NumberFormat("en-US", {
      maximumFractionDigits: 2,
    }).format(toNumber(amount));
    return `${currency === "USD" ? "$" : `${currency} `}${foreign}`;
  }
  return formatWon(amount);
};

const daysInMonth = (year, month) => new Date(year, month, 0).getDate();

/** 매월 결제일(1~31) 기준으로 오늘 이후의 다음 결제일을 계산한다. 말일이 짧은 달은 마지막 날로 당긴다. */
const nextPayDate = (payDay, from = new Date()) => {
  if (!payDay) return null;
  const today = new Date(from.getFullYear(), from.getMonth(), from.getDate());
  const clampedDay = (year, month) =>
    Math.min(payDay, daysInMonth(year, month));
  let year = today.getFullYear();
  let month = today.getMonth() + 1;
  let candidate = new Date(year, month - 1, clampedDay(year, month));
  if (candidate < today) {
    month += 1;
    if (month > 12) {
      month = 1;
      year += 1;
    }
    candidate = new Date(year, month - 1, clampedDay(year, month));
  }
  return candidate;
};

const daysUntil = (date) => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const target = new Date(date);
  target.setHours(0, 0, 0, 0);
  return Math.round((target - today) / 86400000);
};

const paymentSchedule = (payDay) => {
  const date = nextPayDate(payDay);
  if (!date) return null;
  return { date, daysUntil: daysUntil(date) };
};

const scheduleBadge = (daysLeft) => {
  if (daysLeft === 0) return { label: "오늘 결제일", tone: "today" };
  if (daysLeft <= 3) return { label: `D-${daysLeft}`, tone: "soon" };
  return { label: `D-${daysLeft}`, tone: "" };
};

/** 서버가 계산한 이번 주기 이행 상태(view/fixedExpense.md §4). 카드 미연동·이체형은 null(배지 없음). */
const PAYMENT_STATUS_BADGE = {
  PAID: { label: "정상", tone: "paid" },
  DUE_SOON: { label: "결제예정", tone: "today" },
  MISSED: { label: "미납", tone: "missed" },
};

const paymentStatusBadge = (paymentStatus) =>
  PAYMENT_STATUS_BADGE[paymentStatus] || null;

const formatMonthDay = (date) =>
  date ? `${date.getMonth() + 1}월 ${date.getDate()}일` : "—";

const METHOD_LABEL = { CARD: "카드", TRANSFER: "계좌이체" };

const serviceInitial = (name = "") => {
  const trimmed = name.trim();
  if (!trimmed) return "?";
  const firstWord = trimmed.split(/\s+/)[0];
  return /^[A-Za-z]/.test(firstWord)
    ? firstWord.slice(0, 2).toUpperCase()
    : trimmed.slice(0, 1);
};

const ICON_TONES = ["red", "blue", "purple", "green"];
const toneForId = (id) => ICON_TONES[toNumber(id) % ICON_TONES.length];

export {
  formatAmount,
  formatMonthDay,
  formatWon,
  METHOD_LABEL,
  nextPayDate,
  paymentSchedule,
  paymentStatusBadge,
  scheduleBadge,
  serviceInitial,
  toneForId,
  toNumber,
};
