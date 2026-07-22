import { useCallback, useEffect, useState } from "react";
import {
  dismissCoachingCard,
  getCardConnections,
  getCategories,
  getCategorySummary,
  getCoachingCards,
  getFixedExpenseCandidates,
  getInitialSyncStatus,
  getRecentDays,
  getSpendingGuideSummary,
  getTodayTransactions,
  retryInitialSync,
  syncCardTransactions,
} from "../../../api/spendingGuideApi";
import { errorMessage, INITIAL_SYNC_IN_PROGRESS } from "../spendingGuideUtils";

const EMPTY_DATA = {
  summary: null,
  today: null,
  recent: null,
  category: null,
  candidates: null,
  coaching: null,
  categories: null,
  connections: null,
};

function useSpendingGuide() {
  const [data, setData] = useState(EMPTY_DATA);
  const [sectionErrors, setSectionErrors] = useState({});
  const [pageError, setPageError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isEntryOpen, setIsEntryOpen] = useState(false);
  const [isBudgetOpen, setIsBudgetOpen] = useState(false);
  const [isIncomeAdjustmentOpen, setIsIncomeAdjustmentOpen] = useState(false);
  const [detailId, setDetailId] = useState(null);
  const [editingTransaction, setEditingTransaction] = useState(null);
  const [pendingCoachId, setPendingCoachId] = useState(null);
  const [initialSyncState, setInitialSyncState] = useState(null);
  const [retryingConnectionId, setRetryingConnectionId] = useState(null);
  const [isSyncing, setIsSyncing] = useState(false);

  const loadGuide = useCallback(async () => {
    setIsLoading(true);
    setPageError("");
    try {
      const summary = await getSpendingGuideSummary();
      if (summary.setupRequired) {
        setData({ ...EMPTY_DATA, summary });
        setSectionErrors({});
        setInitialSyncState(null);
        return;
      }
      const syncState = await getInitialSyncStatus();
      if (
        INITIAL_SYNC_IN_PROGRESS.has(syncState?.overallStatus) ||
        syncState?.overallStatus === "FAILED"
      ) {
        setData({ ...EMPTY_DATA, summary });
        setSectionErrors({});
        setInitialSyncState(syncState);
        return;
      }
      const requests = {
        today: getTodayTransactions(),
        recent: getRecentDays(),
        category: getCategorySummary(),
        candidates: getFixedExpenseCandidates(),
        coaching: getCoachingCards(),
        categories: getCategories(),
        connections: getCardConnections(),
      };
      const entries = Object.entries(requests);
      const results = await Promise.allSettled(
        entries.map(([, request]) => request),
      );
      const nextData = { ...EMPTY_DATA, summary };
      const nextErrors = {};
      results.forEach((result, index) => {
        const key = entries[index][0];
        if (result.status === "fulfilled") nextData[key] = result.value;
        else
          nextErrors[key] = errorMessage(
            result.reason,
            "데이터를 불러오지 못했습니다.",
          );
      });
      setData(nextData);
      setSectionErrors(nextErrors);
      setInitialSyncState(syncState);
    } catch (error) {
      setPageError(errorMessage(error, "소비가이드를 불러오지 못했습니다."));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadGuide();
  }, [loadGuide]);

  useEffect(() => {
    if (!INITIAL_SYNC_IN_PROGRESS.has(initialSyncState?.overallStatus))
      return undefined;
    const intervalId = window.setInterval(async () => {
      try {
        const nextState = await getInitialSyncStatus();
        if (
          nextState?.overallStatus === "COMPLETED" ||
          nextState?.overallStatus == null
        ) {
          window.clearInterval(intervalId);
          await loadGuide();
          return;
        }
        setInitialSyncState(nextState);
      } catch {
        // 초기 수집은 백엔드에서 계속 진행되므로 다음 폴링에서 다시 확인한다.
      }
    }, 5000);
    return () => window.clearInterval(intervalId);
  }, [initialSyncState?.overallStatus, loadGuide]);

  const handleCardConnected = useCallback(
    async (connection) => {
      setInitialSyncState({
        overallStatus: connection?.initialSyncStatus || "NOT_STARTED",
        connections: connection ? [connection] : [],
      });
      await loadGuide();
    },
    [loadGuide],
  );

  const handleInitialSyncRetry = async (connectionId) => {
    setRetryingConnectionId(connectionId);
    try {
      await retryInitialSync(connectionId);
      setInitialSyncState((current) => ({
        ...current,
        overallStatus: "FETCHING",
      }));
    } catch (error) {
      setPageError(
        errorMessage(error, "카드 소비내역을 다시 수집하지 못했습니다."),
      );
    } finally {
      setRetryingConnectionId(null);
    }
  };

  const expandToday = async () => {
    try {
      const today = await getTodayTransactions(8);
      setData((current) => ({ ...current, today }));
    } catch (error) {
      setSectionErrors((current) => ({
        ...current,
        today: errorMessage(error, "오늘 거래를 더 불러오지 못했습니다."),
      }));
    }
  };

  const handleManualSync = async () => {
    setIsSyncing(true);
    setPageError("");
    try {
      const result = await syncCardTransactions();
      if (result?.locked) {
        setPageError(
          "카드내역 동기화가 이미 진행 중이에요. 잠시 후 다시 시도해주세요.",
        );
        return;
      }
      await loadGuide();
    } catch (error) {
      setPageError(errorMessage(error, "카드내역을 동기화하지 못했습니다."));
    } finally {
      setIsSyncing(false);
    }
  };

  const handleDismiss = async (cardId, dismissType) => {
    if (
      dismissType === "HIDE" &&
      !window.confirm("이 코칭 카드를 그만 볼까요?")
    )
      return;
    setPendingCoachId(cardId);
    try {
      await dismissCoachingCard(cardId, dismissType);
      setData((current) => ({
        ...current,
        coaching: {
          ...current.coaching,
          items: current.coaching.items.filter((item) => item.id !== cardId),
        },
      }));
    } catch (error) {
      setSectionErrors((current) => ({
        ...current,
        coaching: errorMessage(error, "코칭 응답을 저장하지 못했습니다."),
      }));
    } finally {
      setPendingCoachId(null);
    }
  };

  return {
    data,
    detailId,
    editingTransaction,
    expandToday,
    handleCardConnected,
    handleDismiss,
    handleInitialSyncRetry,
    handleManualSync,
    initialSyncState,
    isBudgetOpen,
    isEntryOpen,
    isIncomeAdjustmentOpen,
    isLoading,
    isSyncing,
    loadGuide,
    pageError,
    pendingCoachId,
    retryingConnectionId,
    sectionErrors,
    setDetailId,
    setEditingTransaction,
    setIsBudgetOpen,
    setIsEntryOpen,
    setIsIncomeAdjustmentOpen,
  };
}

export default useSpendingGuide;
