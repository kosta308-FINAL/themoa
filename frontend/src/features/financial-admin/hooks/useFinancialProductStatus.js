import { useEffect, useState } from "react";
import { getFinancialProductStatus } from "../../../api/financialAdminApi";
import { getApiErrorMessage } from "../../../utils/apiError";

/**
 * 금융상품 현황. 수집·인덱스 갱신을 실행한 뒤 reload()로 숫자를 다시 맞춘다.
 *
 * indexSynced는 "판매중 상품 수 == 인덱스 문서 수"로 판단한다.
 * 인덱스 문서 수를 모르는 경우(null)에는 비교 자체가 불가능하므로 true/false가 아닌 null을 준다.
 */
export const useFinancialProductStatus = () => {
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    getFinancialProductStatus()
      .then((data) => {
        if (active) {
          setStatus(data || null);
        }
      })
      .catch((statusError) => {
        if (active) {
          setError(
            getApiErrorMessage(statusError, "현황을 불러오지 못했어요."),
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
    try {
      setStatus((await getFinancialProductStatus()) || null);
      setError("");
    } catch (statusError) {
      setError(getApiErrorMessage(statusError, "현황을 불러오지 못했어요."));
    }
  };

  const sellingTotal =
    (status?.savings?.selling || 0) + (status?.loans?.selling || 0);
  const indexedCount = status?.indexedDocumentCount;
  const indexSynced =
    status == null || indexedCount == null
      ? null
      : indexedCount === sellingTotal;

  return { status, loading, error, reload, sellingTotal, indexSynced };
};
