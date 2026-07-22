import { Link } from "react-router-dom";
import { formatWon } from "../dashboardUtils";

function SpendingTipCard({ coaching, loading, error }) {
  const tip = coaching?.items?.[0];

  return (
    <div className="tip-card">
      <h3>소비 코칭</h3>
      {loading && !coaching && <div className="dash-loading">소비 코칭을 불러오고 있어요.</div>}
      {error && !coaching && <div className="dash-section-error">소비 코칭을 불러오지 못했어요.</div>}
      {!loading && !error && !tip && (
        <div className="dash-empty-state">
          <strong>현재 확인할 소비 코칭이 없어요.</strong>
          <span>소비 내역이 쌓이면 맞춤 코칭을 확인할 수 있어요.</span>
        </div>
      )}
      {tip && (
        <>
          {error && <div className="dash-section-error">{error}</div>}
          <strong className="tip-title">{tip.title}</strong>
          <p>{tip.body}</p>
          {tip.targetLabel && <span className="tip-target">{tip.targetLabel}</span>}
          {tip.estimatedSaving != null && (
            <span className="tip-saving">예상 절약액 {formatWon(tip.estimatedSaving)}</span>
          )}
          <Link to="/dashboard/spending" className="tip-cta">
            소비가이드 보기
          </Link>
        </>
      )}
    </div>
  );
}

export default SpendingTipCard;
