import { useState } from "react";
import {
  collectFinancialProducts,
  rebuildFinancialEmbeddings,
} from "../../../api/financialAdminApi";
import { getApiErrorMessage } from "../../../utils/apiError";

const FORBIDDEN_MESSAGE =
  "ADMIN 권한이 없는 계정이에요. 관리자 계정으로 다시 로그인해 주세요.";

const errorMessage = (error, fallback) =>
  error?.response?.status === 403
    ? FORBIDDEN_MESSAGE
    : getApiErrorMessage(error, fallback);

/**
 * 금융상품 수집 / 검색 인덱스 갱신 실행 상태.
 * 두 작업 모두 오래 걸리고 서로 순서가 있으므로(수집 → 신규가 있으면 인덱스 갱신)
 * 진행 중에는 다른 작업을 시작하지 못하게 한다.
 */
export const useFinancialProductAdmin = () => {
  const [collectResult, setCollectResult] = useState(null);
  const [embedResult, setEmbedResult] = useState(null);
  const [running, setRunning] = useState("");
  const [error, setError] = useState("");

  const runCollect = async () => {
    if (running) {
      return;
    }
    setRunning("collect");
    setError("");
    setEmbedResult(null);
    try {
      setCollectResult(await collectFinancialProducts());
    } catch (collectError) {
      setCollectResult(null);
      setError(errorMessage(collectError, "상품 수집에 실패했어요."));
    } finally {
      setRunning("");
    }
  };

  const runRebuild = async () => {
    if (running) {
      return;
    }
    setRunning("rebuild");
    setError("");
    try {
      setEmbedResult(await rebuildFinancialEmbeddings());
    } catch (rebuildError) {
      setEmbedResult(null);
      setError(errorMessage(rebuildError, "검색 인덱스 갱신에 실패했어요."));
    } finally {
      setRunning("");
    }
  };

  return { collectResult, embedResult, running, error, runCollect, runRebuild };
};
