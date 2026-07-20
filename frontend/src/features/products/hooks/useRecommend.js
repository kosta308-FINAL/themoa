import { useState } from "react";
import { postRecommend } from "../../../api/productsApi";

const errorMessage = (error) =>
  error?.response?.data?.message || "추천 요청을 처리하지 못했습니다.";

/**
 * 맞춤 금융상품 추천 호출/상태 관리. 페이지는 runRecommend(payload)만 호출하고,
 * 로딩·오류·결과·검색여부(searched) 분기는 이 훅이 담당한다.
 */
export const useRecommend = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [searched, setSearched] = useState(false);

  const runRecommend = async (payload) => {
    setLoading(true);
    setError("");
    try {
      const result = await postRecommend(payload);
      setData(result);
      setSearched(true);
    } catch (recommendError) {
      setData(null);
      setError(errorMessage(recommendError));
      setSearched(true);
    } finally {
      setLoading(false);
    }
  };

  return { data, loading, error, searched, runRecommend };
};
