export const WON = new Intl.NumberFormat("ko-KR");

export const toNumber = (value) => {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
};

export const formatWon = (value) => `${WON.format(toNumber(value))}원`;

export const GENDER_LABELS = { MALE: "남성", FEMALE: "여성" };

export const INCOME_TYPE_LABELS = { SALARY: "월급제", HOURLY: "알바(시급제)" };

export const ENTRY_MODE_LABELS = { MANUAL: "수기 입력", CARD: "카드 연동" };

export const TERMS_TYPE_LABELS = {
  SERVICE_TERMS: "서비스 이용약관",
  PRIVACY_POLICY: "개인정보 수집·이용 동의",
  DATA_COLLECTION: "데이터 수집·활용 동의(선택)",
};

export const CONNECTION_STATUS_LABELS = {
  ACTIVE: "정상 연결",
  ERROR: "재연결 필요",
  LOCKED: "카드사 계정 잠김",
};

export const formatDate = (value) => {
  if (!value) return "-";
  return value.slice(0, 10);
};

export const formatDateTime = (value) => {
  if (!value) return "-";
  return value.replace("T", " ").slice(0, 16);
};
