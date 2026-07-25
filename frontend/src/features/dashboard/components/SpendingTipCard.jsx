import { Link } from "react-router-dom";
import DashEmptyState from "./DashEmptyState";
import DashSectionError from "./DashSectionError";
import { formatWon } from "../dashboardUtils";

function SpendingTipCard({ coaching, loading, error }) {
  const tip = coaching?.items?.[0];

  return (
    <div className="tip-card">
      <h3>소비 코칭</h3>
      {loading && !coaching && <div className="dash-loading">소비 코칭을 불러오고 있어요.</div>}
      {!loading && !tip && (
        <DashEmptyState
          icon="sparkle"
          title="현재 확인할 소비 코칭이 없어요"
          description="소비가이드를 설정하고 소비 내역이 쌓이면 맞춤 코칭을 확인할 수 있어요."
        />
      )}
      {tip && (
        <>
          {error && <DashSectionError message={error} />}
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
