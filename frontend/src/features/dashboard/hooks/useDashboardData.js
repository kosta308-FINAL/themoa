import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { getBookmarks } from "../../../api/bookmarkApi";
import { getMyPage } from "../../../api/mypageApi";
import { getPolicyBookmarks } from "../../../api/policyApi";
import {
  getCategorySummary,
  getCoachingCards,
  getConsumptionHistoryTransactions,
  getSpendingGuideSummary,
  getTodayTransactions,
} from "../../../api/spendingGuideApi";
import { getApiErrorMessage } from "../../../utils/apiError";

const EMPTY_DATA = {
  myPage: null,
  summary: null,
  today: null,
  recentTransactions: null,
  category: null,
  coaching: null,
  productBookmarks: null,
  policyBookmarks: null,
};

const REQUESTS = {
  myPage: () => getMyPage(),
  summary: () => getSpendingGuideSummary(),
  today: () => getTodayTransactions(1),
  recentTransactions: () => getConsumptionHistoryTransactions({ page: 0, size: 5 }),
  category: () => getCategorySummary(),
  coaching: () => getCoachingCards(),
  productBookmarks: () => getBookmarks(),
  policyBookmarks: () => getPolicyBookmarks(),
};

const FALLBACK_MESSAGES = {
  myPage: "회원 정보를 불러오지 못했어요.",
  summary: "소비 현황을 불러오지 못했어요.",
  today: "오늘 거래 건수를 불러오지 못했어요.",
  recentTransactions: "최근 소비 내역을 불러오지 못했어요.",
  category: "소비 분석을 불러오지 못했어요.",
  coaching: "소비 코칭을 불러오지 못했어요.",
  productBookmarks: "관심 금융상품을 불러오지 못했어요.",
  policyBookmarks: "관심 정책을 불러오지 못했어요.",
};

export function useDashboardData() {
  const [data, setData] = useState(EMPTY_DATA);
  const [sectionErrors, setSectionErrors] = useState({});
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [lastUpdatedAt, setLastUpdatedAt] = useState(null);
  const hasLoadedData = useRef(false);
  const mounted = useRef(true);

  const reload = useCallback(async () => {
    const refreshing = hasLoadedData.current;
    if (refreshing) {
      setIsRefreshing(true);
    } else {
      setIsLoading(true);
    }

    const entries = Object.entries(REQUESTS);
    const results = await Promise.allSettled(entries.map(([, request]) => request()));
    let successCount = 0;
    const nextErrors = {};
    const nextValues = {};

    results.forEach((result, index) => {
      const [key] = entries[index];
      if (result.status === "fulfilled") {
        successCount += 1;
        nextValues[key] = result.value;
        return;
      }
      nextErrors[key] = getApiErrorMessage(result.reason, FALLBACK_MESSAGES[key]);
    });

    if (!mounted.current) return;
    setData((current) => ({
      ...current,
      ...nextValues,
    }));
    setSectionErrors(nextErrors);
    if (successCount > 0) {
      hasLoadedData.current = true;
      setLastUpdatedAt(new Date());
    }
    setIsLoading(false);
    setIsRefreshing(false);
  }, []);

  useEffect(() => {
    mounted.current = true;
    const timer = window.setTimeout(() => {
      if (mounted.current) reload();
    }, 0);
    return () => {
      mounted.current = false;
      window.clearTimeout(timer);
    };
  }, [reload]);

  return useMemo(() => ({
    data,
    sectionErrors,
    isLoading,
    isRefreshing,
    lastUpdatedAt,
    reload,
  }), [data, isLoading, isRefreshing, lastUpdatedAt, reload, sectionErrors]);
}
