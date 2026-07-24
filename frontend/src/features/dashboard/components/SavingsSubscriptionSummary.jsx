import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getSubscriptions } from "../../../api/savingsSubscriptionApi";
import "./SavingsSubscriptionSummary.css";

const won = (value) =>
  value == null ? "-" : `${Number(value).toLocaleString("ko-KR")}원`;

/**
 * 홈 대시보드용 "가입한 예·적금" 요약(조회 전용). 등록·수정·삭제는 마이페이지에서 한다.
 * 만기 예상 합계와 미충족 우대조건이 있는 상품을 한눈에 보여준다.
 */
function SavingsSubscriptionSummary() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    let active = true;

    const fetchList = () => {
      getSubscriptions()
        .then((data) => {
          if (active) {
            setItems(data || []);
            setError(false);
          }
        })
        .catch(() => {
          if (active) setError(true);
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

  const totalMaturity = items.reduce(
    (sum, item) => sum + (Number(item.expectedMaturityAmount) || 0),
    0,
  );
  const totalCurrent = items.reduce(
    (sum, item) => sum + (Number(item.currentValue) || 0),
    0,
  );

  return (
    <div className="widget-panel">
      <div className="widget-panel-header">
        <h3>가입한 예·적금</h3>
        <Link
          to="/dashboard/mypage?tab=subscriptions"
          className="sub-summary-manage"
        >
          관리
        </Link>
      </div>

      {loading && (
        <div className="dash-loading">가입 상품을 불러오고 있어요.</div>
      )}

      {!loading && error && (
        <div className="dash-section-error">가입 상품을 불러오지 못했어요.</div>
      )}

      {!loading && !error && items.length === 0 && (
        <div className="dash-empty-state">
          <strong>아직 등록한 가입 상품이 없어요.</strong>
          <Link to="/dashboard/products/search">금융상품 찾기</Link>
        </div>
      )}

      {!loading && !error && items.length > 0 && (
        <>
          <div className="sub-summary-totals">
            <div className="sub-summary-total sub-summary-total-current">
              <span>현재 평가 합계</span>
              <strong>{won(totalCurrent)}</strong>
            </div>
            <div className="sub-summary-total">
              <span>만기 예상 합계</span>
              <strong>{won(totalMaturity)}</strong>
            </div>
          </div>
          <ul className="sub-summary-list">
            {items.slice(0, 3).map((item) => (
              <li key={item.id}>
                <div className="sub-summary-item-main">
                  <h4>{item.productName}</h4>
                  <p>
                    {item.companyName} · {item.appliedRate}% · {item.termMonth}
                    개월
                  </p>
                </div>
                <div className="sub-summary-item-side">
                  <span className="sub-summary-current">
                    현재 {won(item.currentValue)}
                  </span>
                  <strong>만기 {won(item.expectedMaturityAmount)}</strong>
                  {item.unmetConditionCount > 0 && (
                    <span className="sub-summary-warn">
                      우대 {item.unmetConditionCount}개 미충족
                    </span>
                  )}
                </div>
              </li>
            ))}
          </ul>
          {items.length > 3 && (
            <Link
              to="/dashboard/mypage?tab=subscriptions"
              className="sub-summary-more"
            >
              +{items.length - 3}개 더 보기
            </Link>
          )}
        </>
      )}
    </div>
  );
}

export default SavingsSubscriptionSummary;
