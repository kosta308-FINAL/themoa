import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  createCalendarSchedule,
  deleteCalendarSchedule,
  getCalendarEvents,
  updateCalendarSchedule,
} from "../../../api/calendarApi";
import { getApiErrorMessage } from "../../../utils/apiError";

export function useCalendar({ startDate, endDate }) {
  const [events, setEvents] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [mutationError, setMutationError] = useState("");
  const mounted = useRef(true);
  const hasLoaded = useRef(false);

  const reload = useCallback(async () => {
    if (!startDate || !endDate) return;
    if (!hasLoaded.current) {
      setIsLoading(true);
    }
    setError("");
    try {
      const data = await getCalendarEvents({ startDate, endDate });
      if (!mounted.current) return;
      setEvents(data?.items || []);
      hasLoaded.current = true;
    } catch (requestError) {
      if (!mounted.current) return;
      setError(getApiErrorMessage(requestError, "캘린더 일정을 불러오지 못했어요."));
    } finally {
      if (mounted.current) {
        setIsLoading(false);
      }
    }
  }, [endDate, startDate]);

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

  const runMutation = useCallback(
    async (request) => {
      setIsSaving(true);
      setMutationError("");
      try {
        const result = await request();
        await reload();
        return result;
      } catch (requestError) {
        const message = getApiErrorMessage(requestError, "일정을 저장하지 못했어요.");
        if (mounted.current) {
          setMutationError(message);
        }
        throw requestError;
      } finally {
        if (mounted.current) {
          setIsSaving(false);
        }
      }
    },
    [reload],
  );

  const createSchedule = useCallback(
    (payload) => runMutation(() => createCalendarSchedule(payload)),
    [runMutation],
  );

  const updateSchedule = useCallback(
    (scheduleId, payload) => runMutation(() => updateCalendarSchedule(scheduleId, payload)),
    [runMutation],
  );

  const deleteSchedule = useCallback(
    (scheduleId) => runMutation(() => deleteCalendarSchedule(scheduleId)),
    [runMutation],
  );

  return useMemo(
    () => ({
      events,
      isLoading,
      error,
      isSaving,
      mutationError,
      reload,
      createSchedule,
      updateSchedule,
      deleteSchedule,
    }),
    [
      createSchedule,
      deleteSchedule,
      error,
      events,
      isLoading,
      isSaving,
      mutationError,
      reload,
      updateSchedule,
    ],
  );
}
