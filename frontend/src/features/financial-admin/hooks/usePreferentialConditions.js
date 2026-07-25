import { useEffect, useState } from "react";
import {
  getConditionCache,
  getConditionReviewList,
  refreshConditionCache,
  reparseConditionCache,
  searchConditionProducts,
  updateConditionCache,
} from "../../../api/financialAdminApi";
import { getApiErrorMessage } from "../../../utils/apiError";

/**
 * 우대조건 파싱 캐시 관리 상태.
 * - 재검토 필요(stale) 목록 조회
 * - 캐시 전체 재생성(LLM 재파싱, 오래 걸림 → 중복 실행 방지)
 * - 상품별 캐시 조회/수동 수정(저장 시 잠금)
 * 저장·재생성 후에는 재검토 목록을 다시 받아 stale 상태를 최신으로 맞춘다.
 */
export const usePreferentialConditions = () => {
  const [reviewItems, setReviewItems] = useState([]);
  const [reviewLoading, setReviewLoading] = useState(true);
  const [error, setError] = useState("");

  const [refreshing, setRefreshing] = useState(false);
  const [refreshResult, setRefreshResult] = useState(null);

  const [detail, setDetail] = useState(null);
  const [lookupBusy, setLookupBusy] = useState(false);
  const [lookupError, setLookupError] = useState("");
  const [saving, setSaving] = useState(false);
  const [reparsing, setReparsing] = useState(false);

  const [products, setProducts] = useState([]);
  const [productsLoading, setProductsLoading] = useState(true);

  useEffect(() => {
    let active = true;
    getConditionReviewList()
      .then((data) => {
        if (active) {
          setReviewItems(data || []);
        }
      })
      .catch((loadError) => {
        if (active) {
          setError(
            getApiErrorMessage(loadError, "재검토 목록을 불러오지 못했어요."),
          );
        }
      })
      .finally(() => {
        if (active) {
          setReviewLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, []);

  // 첫 진입 시 keyword 없이 전체 대상 상품을 한 번 받아온다.
  useEffect(() => {
    let active = true;
    searchConditionProducts("")
      .then((data) => {
        if (active) {
          setProducts(data || []);
        }
      })
      .catch(() => {
        // 상품 목록을 못 받아와도 ID 직접 입력 조회는 그대로 동작하므로 조용히 넘어간다.
      })
      .finally(() => {
        if (active) {
          setProductsLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, []);

  const searchProducts = async (keyword) => {
    setProductsLoading(true);
    try {
      setProducts((await searchConditionProducts(keyword)) || []);
    } catch (searchError) {
      setLookupError(
        getApiErrorMessage(searchError, "상품 검색에 실패했어요."),
      );
    } finally {
      setProductsLoading(false);
    }
  };

  const reloadReview = async () => {
    setReviewItems((await getConditionReviewList()) || []);
  };

  const runRefresh = async () => {
    if (refreshing) {
      return;
    }
    setRefreshing(true);
    setError("");
    try {
      const result = await refreshConditionCache();
      setRefreshResult(result || null);
      await reloadReview();
    } catch (refreshError) {
      setError(getApiErrorMessage(refreshError, "캐시 재생성에 실패했어요."));
    } finally {
      setRefreshing(false);
    }
  };

  const lookup = async (productId) => {
    setLookupBusy(true);
    setLookupError("");
    setDetail(null);
    try {
      const data = await getConditionCache(productId);
      setDetail(data || null);
      return data || null;
    } catch (fetchError) {
      setLookupError(
        getApiErrorMessage(fetchError, "해당 상품의 캐시를 찾지 못했어요."),
      );
      return null;
    } finally {
      setLookupBusy(false);
    }
  };

  // 최신 원문으로 재파싱한 초안 항목을 받아 반환한다(저장은 별도 PUT). 실패 시 null.
  const reparse = async (productId) => {
    if (reparsing) {
      return null;
    }
    setReparsing(true);
    setLookupError("");
    try {
      const items = await reparseConditionCache(productId);
      return items || [];
    } catch (reparseError) {
      setLookupError(
        getApiErrorMessage(reparseError, "최신 원문 재파싱에 실패했어요."),
      );
      return null;
    } finally {
      setReparsing(false);
    }
  };

  const save = async (productId, items) => {
    setSaving(true);
    setLookupError("");
    try {
      await updateConditionCache(productId, items);
      // 저장 후 잠금(editedByAdmin)·stale 상태가 바뀌므로 상세와 재검토 목록을 다시 받는다.
      const refreshed = await lookup(productId);
      await reloadReview();
      return refreshed != null;
    } catch (saveError) {
      setLookupError(getApiErrorMessage(saveError, "저장에 실패했어요."));
      return false;
    } finally {
      setSaving(false);
    }
  };

  return {
    reviewItems,
    reviewLoading,
    error,
    refreshing,
    refreshResult,
    runRefresh,
    detail,
    lookupBusy,
    lookupError,
    saving,
    reparsing,
    lookup,
    reparse,
    save,
    products,
    productsLoading,
    searchProducts,
  };
};
