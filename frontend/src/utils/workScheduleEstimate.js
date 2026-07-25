export const TAX_OPTIONS = [
  { value: "INSURANCE", label: "4대보험", rate: 0.097 },
  { value: "FREELANCE", label: "3.3%", rate: 0.033 },
  { value: "NONE", label: "없음", rate: 0 },
];

export const HOUR_OPTIONS = Array.from({ length: 24 }, (_, i) => i);
export const MINUTE_OPTIONS = [0, 30];

export const AVG_WEEKS_PER_MONTH = 4.345;

export const toDecimalHours = (hour, minute) => {
  const value = hour + (minute === 30 ? 0.5 : 0);
  return String(value);
};

export const parseDecimalHours = (value) => {
  const n = Number(value);
  if (!Number.isFinite(n) || n <= 0) return { hour: 0, minute: 0 };
  const hour = Math.floor(n);
  const minute = Math.round((n - hour) * 60) >= 30 ? 30 : 0;
  return { hour, minute };
};

// 시(hour)만 바꾸고 기존 분(minute)은 그대로 유지한다.
export const withHour = (value, hour) => {
  const { minute } = parseDecimalHours(value);
  return toDecimalHours(hour, minute);
};

// 분(minute)만 바꾸고 기존 시(hour)는 그대로 유지한다.
export const withMinute = (value, minute) => {
  const { hour } = parseDecimalHours(value);
  return toDecimalHours(hour, minute);
};

// 트리거 버튼에 쓰는 "시간" 표시. 아직 아무 값도 없을 때만 안내 문구를 보여주고,
// 명시적으로 0시를 고른 경우("0")에는 0시간으로 그대로 보여준다.
export const formatHourLabel = (value) => {
  if (value === "" || value == null) return "시간 선택";
  const { hour } = parseDecimalHours(value);
  return `${hour}시간`;
};

export const roundToThousand = (amount) => Math.round(amount / 1000) * 1000;

// 세전 추정 급여에 선택한 공제 유형(4대보험/3.3%)의 정률만 적용한 근사치.
// 주휴수당 등 다른 가산요소는 반영하지 않으며, 정확한 금액은 실입금 후 수입 내역에서 조정한다.
export const estimateNetPay = (grossAmount, taxType) => {
  const option = TAX_OPTIONS.find((item) => item.value === taxType);
  const rate = option ? option.rate : 0;
  return roundToThousand(grossAmount * (1 - rate));
};
