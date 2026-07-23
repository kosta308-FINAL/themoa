import { useState } from "react";
import { explainFinancialSearch } from "../../../api/financialAdminApi";
import { getApiErrorMessage } from "../../../utils/apiError";

/**
 * 검색 품질 점검 상태. 예시 검색어 버튼에서도 바로 실행할 수 있게
 * run()이 검색어를 인자로 받는다.
 */
export const useSearchExplain = () => {
  const [query, setQuery] = useState("");
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const run = async (nextQuery = query) => {
    const trimmed = nextQuery.trim();
    if (!trimmed) {
      setError("점검할 검색어를 입력해 주세요.");
      return;
    }

    setQuery(trimmed);
    setLoading(true);
    setError("");
    try {
      setResult(await explainFinancialSearch(trimmed));
    } catch (explainError) {
      setResult(null);
      setError(getApiErrorMessage(explainError, "검색 점검에 실패했어요."));
    } finally {
      setLoading(false);
    }
  };

  return { query, setQuery, result, loading, error, run };
};
