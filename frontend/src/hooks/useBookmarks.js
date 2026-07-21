import { useCallback, useEffect, useMemo, useState } from "react";
import {
  addBookmark,
  getBookmarkTargets,
  removeBookmark,
} from "../api/bookmarkApi";

const targetKey = (targetType, targetId) => `${targetType}:${targetId}`;

/**
 * 상품 목록 화면(추천·검색)에서 쓰는 북마크 상태.
 * 진입 시 내 북마크 대상 목록을 한 번만 받아와 별표 표시를 판단하고,
 * 토글 결과는 서버 응답을 기다린 뒤 반영한다(실패하면 표시가 어긋나지 않도록).
 */
export const useBookmarks = () => {
  const [targets, setTargets] = useState([]);
  const [busyKey, setBusyKey] = useState("");
  const [toast, setToast] = useState(null);

  // 진입 시 + 화면이 다시 활성화될 때 내 북마크 대상을 받아온다.
  // 다른 탭/창(마이페이지 등)에서 해제하고 돌아왔을 때 별표가 옛 상태로 남지 않게 하기 위함.
  useEffect(() => {
    let active = true;

    const fetchTargets = () => {
      getBookmarkTargets()
        .then((data) => {
          if (active) {
            setTargets(data || []);
          }
        })
        .catch(() => {
          // 북마크 목록을 못 받아와도 상품 목록 자체는 보여줘야 하므로 조용히 넘어간다.
          // (별표가 비어 보일 뿐이고, 토글은 그대로 동작한다)
        });
    };

    const refreshIfVisible = () => {
      if (document.visibilityState === "visible") {
        fetchTargets();
      }
    };

    fetchTargets();
    window.addEventListener("focus", refreshIfVisible);
    document.addEventListener("visibilitychange", refreshIfVisible);

    return () => {
      active = false;
      window.removeEventListener("focus", refreshIfVisible);
      document.removeEventListener("visibilitychange", refreshIfVisible);
    };
  }, []);

  const bookmarkedKeys = useMemo(
    () => new Set(targets.map((t) => targetKey(t.targetType, t.targetId))),
    [targets],
  );

  const isBookmarked = useCallback(
    (targetType, targetId) =>
      bookmarkedKeys.has(targetKey(targetType, targetId)),
    [bookmarkedKeys],
  );

  const isBusy = useCallback(
    (targetType, targetId) => busyKey === targetKey(targetType, targetId),
    [busyKey],
  );

  const showToast = (message) => setToast({ message, id: Date.now() });

  const toggleBookmark = async (targetType, targetId) => {
    const key = targetKey(targetType, targetId);
    if (busyKey) {
      return;
    }
    const bookmarked = bookmarkedKeys.has(key);
    setBusyKey(key);
    try {
      if (bookmarked) {
        await removeBookmark({ targetType, targetId });
        setTargets((prev) =>
          prev.filter((t) => targetKey(t.targetType, t.targetId) !== key),
        );
        showToast("북마크에 해제되었습니다.");
      } else {
        await addBookmark({ targetType, targetId });
        setTargets((prev) => [...prev, { targetType, targetId }]);
        showToast("북마크에 등록 되었습니다.");
      }
    } catch {
      showToast("북마크 처리에 실패했습니다.");
    } finally {
      setBusyKey("");
    }
  };

  return {
    isBookmarked,
    isBusy,
    toggleBookmark,
    toast,
    clearToast: () => setToast(null),
  };
};
