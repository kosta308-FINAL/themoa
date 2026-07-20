export const mockUser = {
  name: "수지",
  lastUpdated: "5분 전",
};

// hasFinancialInfo=false 면 카드가 "정보 입력 유도" 상태로 표시됨
export const mockFinancialInfo = {
  hasFinancialInfo: true,
  todayAvailable: "34,200원",
  todayBudget: "이번 달 예산 2,000,000원",
  monthSpending: "1,248,500원",
  monthSpendingDiff: "▼ 128,000원 (전월 대비)",
  goalRate: 62,
  goalDetail: "620만원 / 1,000만원",
  creditScore: 842,
  creditPercentile: "상위 12%",
};

export const popularSearches = [
  "청년 전세자금 대출",
  "국민내일배움카드",
  "ISA 계좌",
  "청년도약계좌",
];

export const aiSummary = {
  productCount: 3,
  policyCount: 5,
  tipCount: 2,
};

export const statsStrip = [
  { icon: "people", label: "누적 사용자 수", value: "128,000+", unit: "명" },
  { icon: "building", label: "연결 금융기관", value: "67", unit: "개" },
  { icon: "check", label: "추천 성공률", value: "96.3", unit: "%" },
  { icon: "sparkle", label: "절약/혜택 금액", value: "2,340", unit: "억원+" },
];

export const spendingBreakdown = {
  total: "1,248,500원",
  totalLabel: "이번 달",
  categories: [
    { label: "식비", amount: "399,500원", percent: 32, color: "#16a34a" },
    { label: "교통", amount: "224,300원", percent: 18, color: "#34d399" },
    { label: "쇼핑", amount: "187,200원", percent: 15, color: "#60a5fa" },
    { label: "문화/여가", amount: "149,800원", percent: 12, color: "#c084fc" },
    { label: "주거", amount: "124,000원", percent: 10, color: "#fb923c" },
    { label: "기타", amount: "163,700원", percent: 13, color: "#facc15" },
  ],
};

export const recommendedProduct = {
  badge: "추천 적금",
  name: "우리 청년 우대 적금",
  tags: ["비대면", "우대금리", "높은 금리"],
  rateLabel: "최고 연",
  rate: "4.50%",
};

export const policyRecommendations = [
  {
    icon: "building",
    title: "청년 월세 지원",
    detail: "월 20만원씩 12개월 지원",
    status: "신청 가능",
  },
  {
    icon: "user",
    title: "국가장학금",
    detail: "연 최대 700만원 지원",
    status: "신청 가능",
  },
  {
    icon: "check",
    title: "청년내일채움공제",
    detail: "2년 만기 시 최대 1,200만원",
    status: "신청 가능",
  },
];

export const recentActivity = [
  {
    icon: "sparkle",
    title: "스타벅스 강남점",
    category: "카페/간식",
    amount: "-6,200원",
    time: "오늘 14:30",
    negative: true,
  },
  {
    icon: "building",
    title: "KB 국민은행 적금 가입",
    category: "금융상품",
    amount: "+500,000원",
    time: "어제 11:20",
    negative: false,
  },
];

export const spendingTip = {
  title: "AI 소비 팁",
  message:
    "이번 달 커피 소비가 지난 달보다 23% 증가했어요. 월 3만원 절약할 수 있는 방법을 알아볼까요?",
  cta: "팁 보러가기",
};

export const inviteFriend = {
  title: "친구 초대하고 혜택 받기",
  message: "친구가 가입하면 5,000P를 드려요!",
  cta: "초대하기",
};
