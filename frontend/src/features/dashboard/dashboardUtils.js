const WON = new Intl.NumberFormat("ko-KR");
const RATE = new Intl.NumberFormat("ko-KR", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export const toNumber = (value) => {
  if (value == null || value === "") return null;
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
};

export const formatWon = (value) => {
  const number = toNumber(value);
  return number == null ? "미설정" : `${WON.format(number)}원`;
};

export const formatCount = (value, unit = "건") => {
  const number = toNumber(value);
  return number == null ? "확인 불가" : `${WON.format(number)}${unit}`;
};

export const formatRate = (rate) => {
  const number = toNumber(rate);
  return number == null ? "금리 정보 없음" : `연 ${RATE.format(number)}%`;
};

export const formatDateTime = (value) => {
  if (!value) return "";
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return new Intl.DateTimeFormat("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(date);
};

export const formatTransactionAmount = (netAmount) => {
  const number = toNumber(netAmount);
  if (number == null) return "미설정";
  const formatted = `${WON.format(Math.abs(number))}원`;
  if (number > 0) return `-${formatted}`;
  if (number < 0) return `+${formatted}`;
  return "0원";
};

export const formatPolicyPeriod = ({ alwaysOpen, startDate, dueDate }) => {
  if (alwaysOpen) return "상시 신청";
  if (!startDate && !dueDate) return "일정 미정";
  return `${startDate || "일정 미정"} ~ ${dueDate || "일정 미정"}`;
};
