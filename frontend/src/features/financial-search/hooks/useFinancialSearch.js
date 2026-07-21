import { useState } from "react";
import { searchFinancialProducts } from "../../../api/financialSearchApi";

const MIN_QUERY_LENGTH = 2;

const errorMessage = (error) =>
  error?.response?.data?.message || "검색 요청을 처리하지 못했습니다.";

/**
 * 금융상품 검색 상태 관리. 검색어·정렬은 여기서 들고 있고,
 * 대안 검색어(suggestedQueries) 클릭처럼 외부에서 검색어를 바꿔 즉시 검색하는 경우를 위해
 * runSearch가 검색어를 인자로 받을 수 있게 한다.
 */
export const useFinancialSearch = () => {
  const [query, setQuery] = useState("");
  const [sort, setSort] = useState("RELEVANCE");
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [searched, setSearched] = useState(false);

  const runSearch = async (nextQuery = query, nextSort = sort) => {
    const trimmed = nextQuery.trim();
    if (trimmed.length < MIN_QUERY_LENGTH) {
      setError(`검색어를 ${MIN_QUERY_LENGTH}글자 이상 입력해 주세요.`);
      return;
    }

    setQuery(trimmed);
    setSort(nextSort);
    setLoading(true);
    setError("");
    try {
      const result = await searchFinancialProducts({
        query: trimmed,
        sort: nextSort,
      });
      setData(result);
      setSearched(true);
    } catch (searchError) {
      setData(null);
      setError(errorMessage(searchError));
      setSearched(true);
    } finally {
      setLoading(false);
    }
  };

  return {
    query,
    setQuery,
    sort,
    setSort,
    data,
    loading,
    error,
    searched,
    runSearch,
  };
};
