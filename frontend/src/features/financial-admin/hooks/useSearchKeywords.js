import { useEffect, useState } from "react";
import {
  addSearchKeyword,
  deleteSearchKeyword,
  getSearchKeywords,
  resetSearchKeywords,
} from "../../../api/financialAdminApi";
import { getApiErrorMessage } from "../../../utils/apiError";

/**
 * 검색어 해석용 키워드 관리 상태.
 * 추가·삭제·초기화 후에는 목록을 다시 받아온다 — 서버가 부여한 id가 있어야 삭제가 되고,
 * 중복 추가(200)처럼 목록이 그대로인 경우도 있어 응답으로 직접 조립하지 않는다.
 */
export const useSearchKeywords = () => {
  const [demographicGroups, setDemographicGroups] = useState([]);
  const [productIntents, setProductIntents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState("");

  const applyData = (data) => {
    setDemographicGroups(data?.demographicGroups || []);
    setProductIntents(data?.productIntents || []);
  };

  useEffect(() => {
    let active = true;
    getSearchKeywords()
      .then((data) => {
        if (active) {
          applyData(data);
        }
      })
      .catch((loadError) => {
        if (active) {
          setError(
            getApiErrorMessage(loadError, "키워드를 불러오지 못했어요."),
          );
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, []);

  const reload = async () => {
    applyData(await getSearchKeywords());
  };

  const add = async (keywordType, groupKey, keyword) => {
    const trimmed = keyword.trim();
    if (!trimmed) {
      return false;
    }
    setBusy(`add:${groupKey}`);
    setError("");
    try {
      await addSearchKeyword({ keywordType, groupKey, keyword: trimmed });
      await reload();
      return true;
    } catch (addError) {
      setError(getApiErrorMessage(addError, "키워드를 추가하지 못했어요."));
      return false;
    } finally {
      setBusy("");
    }
  };

  const remove = async (keywordId) => {
    setBusy(`remove:${keywordId}`);
    setError("");
    try {
      await deleteSearchKeyword(keywordId);
      await reload();
    } catch (removeError) {
      setError(getApiErrorMessage(removeError, "키워드를 삭제하지 못했어요."));
    } finally {
      setBusy("");
    }
  };

  const reset = async () => {
    setBusy("reset");
    setError("");
    try {
      await resetSearchKeywords();
      await reload();
    } catch (resetError) {
      setError(getApiErrorMessage(resetError, "키워드를 초기화하지 못했어요."));
    } finally {
      setBusy("");
    }
  };

  return {
    demographicGroups,
    productIntents,
    loading,
    error,
    busy,
    add,
    remove,
    reset,
  };
};
