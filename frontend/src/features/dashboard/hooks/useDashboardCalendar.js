import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { getCalendarEvents } from "../../../api/calendarApi";
import { getApiErrorMessage } from "../../../utils/apiError";

const padDatePart = (value) => String(value).padStart(2, "0");

const formatDateKey = (date) =>
  [
    date.getFullYear(),
    padDatePart(date.getMonth() + 1),
    padDatePart(date.getDate()),
  ].join("-");

const getMonday = (date) => {
  const monday = new Date(date);
  const day = monday.getDay();
  const mondayOffset = day === 0 ? -6 : 1 - day;
  monday.setDate(monday.getDate() + mondayOffset);
  monday.setHours(0, 0, 0, 0);
  return monday;
};

export function useDashboardCalendar() {
  const today = useMemo(() => new Date(), []);
  const range = useMemo(() => {
    const monday = getMonday(today);
    const sunday = new Date(monday);
    sunday.setDate(monday.getDate() + 6);
    return {
      startDate: formatDateKey(monday),
      endDate: formatDateKey(sunday),
    };
  }, [today]);
  const [events, setEvents] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState("");
  const mounted = useRef(true);
  const hasLoaded = useRef(false);

  const reload = useCallback(async () => {
    if (hasLoaded.current) {
      setIsRefreshing(true);
    } else {
      setIsLoading(true);
    }
    setError("");
    try {
      const data = await getCalendarEvents(range);
      if (!mounted.current) return;
      setEvents(data?.items || []);
      hasLoaded.current = true;
    } catch (requestError) {
      if (!mounted.current) return;
      setError(getApiErrorMessage(requestError, "이번 주 일정을 불러오지 못했어요."));
    } finally {
      if (mounted.current) {
        setIsLoading(false);
        setIsRefreshing(false);
      }
    }
  }, [range]);

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

  return useMemo(
    () => ({
      events,
      isLoading,
      isRefreshing,
      error,
      reload,
    }),
    [error, events, isLoading, isRefreshing, reload],
  );
}
