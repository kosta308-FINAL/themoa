import { useEffect, useState } from "react";
import {
  deleteSubscription,
  getSubscriptions,
  updateSubscriptionCondition,
} from "../api/savingsSubscriptionApi";

/**
 * 가입한 예·적금 목록 상태(마이페이지 카드·관리 페이지 공용).
 * 최초 로딩 + 화면 재활성화 시 갱신하고, 조건 토글·삭제 후에는 목록을 다시 받아 파생값(현재/만기)을 맞춘다.
 * (효과 본문에서 곧바로 setState 하지 않도록 상태 변경은 응답 콜백에서만 한다)
 */
export const useSavingsSubscriptions = () => {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);

  useEffect(() => {
    let active = true;

    const fetchList = () => {
      getSubscriptions()
        .then((data) => {
          if (active) {
            setItems(data || []);
            setError("");
          }
        })
        .catch(() => {
          if (active) setError("가입 상품을 불러오지 못했습니다.");
        })
        .finally(() => {
          if (active) setLoading(false);
        });
    };

    const refreshIfVisible = () => {
      if (document.visibilityState === "visible") fetchList();
    };

    fetchList();
    window.addEventListener("focus", refreshIfVisible);
    document.addEventListener("visibilitychange", refreshIfVisible);

    return () => {
      active = false;
      window.removeEventListener("focus", refreshIfVisible);
      document.removeEventListener("visibilitychange", refreshIfVisible);
    };
  }, []);

  const reload = async () => {
    setItems((await getSubscriptions()) || []);
  };

  const reloadWithState = async () => {
    setLoading(true);
    setError("");
    try {
      await reload();
    } catch {
      setError("가입 상품을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const toggleCondition = async (condition) => {
    setBusyId(`cond:${condition.id}`);
    try {
      await updateSubscriptionCondition(condition.id, !condition.met);
      await reload();
    } catch {
      setError("우대조건 상태를 변경하지 못했습니다.");
    } finally {
      setBusyId(null);
    }
  };

  const remove = async (item) => {
    setBusyId(`del:${item.id}`);
    try {
      await deleteSubscription(item.id);
      setItems((prev) => prev.filter((v) => v.id !== item.id));
    } catch {
      setError("삭제하지 못했습니다.");
    } finally {
      setBusyId(null);
    }
  };

  return {
    items,
    loading,
    error,
    busyId,
    reload,
    reloadWithState,
    toggleCondition,
    remove,
  };
};
