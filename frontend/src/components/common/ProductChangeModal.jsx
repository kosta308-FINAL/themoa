import { useEffect, useState } from "react";
import { getProductChangeByNotification } from "../../api/productChangeApi";
import { getApiErrorMessage } from "../../utils/apiError";
import "./ProductChangeModal.css";

const NO_VALUE = "정보 없음";

const rate = (value) => (value == null ? NO_VALUE : `${value}%`);

const formatDetectedAt = (isoString) => {
  if (!isoString) {
    return null;
  }
  const detectedAt = new Date(isoString);
  if (Number.isNaN(detectedAt.getTime())) {
    return null;
  }
  return detectedAt.toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
};

/**
 * 관심 상품 변경 안내 팝업. 알림에서 넘어온 notificationId로 그 시점의 변화를 조회해 보여준다.
 * 금리·우대조건·판매종료는 각각 독립적으로 바뀔 수 있어 변경된 항목만 강조한다.
 */
function ProductChangeModal({ notificationId, onClose }) {
  const [change, setChange] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [historyOpen, setHistoryOpen] = useState(false);
  const [conditionOpen, setConditionOpen] = useState(false);

  useEffect(() => {
    let active = true;
    getProductChangeByNotification(notificationId)
      .then((data) => {
        if (active) {
          setChange(data || null);
        }
      })
      .catch((loadError) => {
        if (active) {
          setError(
            getApiErrorMessage(loadError, "변경 내역을 불러오지 못했어요."),
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
  }, [notificationId]);

  useEffect(() => {
    const handleKeyDown = (event) => {
      if (event.key === "Escape") {
        onClose();
      }
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  const detectedAt = formatDetectedAt(change?.createdAt);
  // 지금 보고 있는 변경 건 외에 과거 이력이 더 있을 때만 펼치기를 노출한다.
  const history = change?.history || [];
  const hasHistory = history.length > 1;

  return (
    <div
      className="pc-backdrop"
      role="presentation"
      onClick={(event) => {
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}
    >
      <div className="pc-modal" role="dialog" aria-label="관심 상품 변경 안내">
        <div className="pc-head">
          <strong>관심 상품 변경 안내</strong>
          <button type="button" onClick={onClose} aria-label="닫기">
            ×
          </button>
        </div>

        <div className="pc-body">
          {loading && <p className="pc-state">변경 내역을 불러오고 있어요.</p>}
          {!loading && error && <p className="pc-error">{error}</p>}

          {!loading && !error && change && (
            <>
              {change.discontinued && (
                <div className="pc-alert">판매종료된 상품이에요.</div>
              )}

              <div className="pc-product">
                <strong>{change.productName}</strong>
                <span>{change.companyName}</span>
              </div>

              <dl className="pc-rows">
                <div
                  className={change.rateChanged ? "pc-row changed" : "pc-row"}
                >
                  <dt>금리</dt>
                  <dd>
                    {change.rateChanged ? (
                      <>
                        <span className="pc-before">
                          {rate(change.previousRate)}
                        </span>
                        <span className="pc-arrow">→</span>
                        <span className="pc-after">
                          {rate(change.currentRate)}
                        </span>
                      </>
                    ) : (
                      <span className="pc-nochange">변경 없음</span>
                    )}
                  </dd>
                </div>

                <div
                  className={
                    change.specialConditionChanged ? "pc-row changed" : "pc-row"
                  }
                >
                  <dt>우대조건</dt>
                  <dd>
                    {change.specialConditionChanged ? (
                      <button
                        type="button"
                        className="pc-toggle"
                        onClick={() => setConditionOpen((prev) => !prev)}
                      >
                        {conditionOpen ? "▾ 접기" : "▸ 변경 내용 보기"}
                      </button>
                    ) : (
                      <span className="pc-nochange">변경 없음</span>
                    )}
                  </dd>
                </div>
              </dl>

              {change.specialConditionChanged && conditionOpen && (
                <div className="pc-condition">
                  <div>
                    <span>이전</span>
                    <p>{change.previousSpecialCondition || NO_VALUE}</p>
                  </div>
                  <div>
                    <span>현재</span>
                    <p>{change.currentSpecialCondition || NO_VALUE}</p>
                  </div>
                </div>
              )}

              {hasHistory && (
                <div className="pc-history">
                  <button
                    type="button"
                    className="pc-toggle"
                    onClick={() => setHistoryOpen((prev) => !prev)}
                  >
                    {historyOpen ? "▾ 이력 접기" : "▸ 이전 변동 이력 보기"}
                  </button>
                  {historyOpen && (
                    <ul>
                      {history.map((item) => (
                        <li key={item.createdAt}>
                          <span className="pc-history-date">
                            {formatDetectedAt(item.createdAt) || NO_VALUE}
                          </span>
                          <span>
                            {item.discontinued
                              ? "판매종료"
                              : `${rate(item.previousRate)} → ${rate(item.currentRate)}`}
                          </span>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )}

              {detectedAt && <p className="pc-detected">{detectedAt} 감지</p>}
            </>
          )}
        </div>

        <div className="pc-foot">
          <button type="button" className="pc-confirm" onClick={onClose}>
            확인
          </button>
        </div>
      </div>
    </div>
  );
}

export default ProductChangeModal;
