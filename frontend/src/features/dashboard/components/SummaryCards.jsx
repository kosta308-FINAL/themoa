import { Link } from "react-router-dom";
import DashboardIcon from "../../../components/common/DashboardIcon";
import AiSummaryBanner from "./AiSummaryBanner";
import { formatWon } from "../dashboardUtils";

function SummarySkeleton() {
  return (
    <>
      {[1, 2, 3, 4].map((item) => (
        <div className="summary-card" key={item}>
          <span className="dash-skeleton dash-skeleton-icon" />
          <span className="dash-skeleton dash-skeleton-line" />
          <span className="dash-skeleton dash-skeleton-value" />
        </div>
      ))}
    </>
  );
}

function SummaryCards({
  summary,
  productBookmarkCount,
  policyBookmarkCount,
  coachingCount,
  loading,
  error,
}) {
  if (loading && !summary) {
    return (
      <div className="summary-cards">
        <SummarySkeleton />
        <AiSummaryBanner
          productBookmarkCount={productBookmarkCount}
          policyBookmarkCount={policyBookmarkCount}
          coachingCount={coachingCount}
        />
      </div>
    );
  }

  if (error && !summary) {
    return (
      <div className="summary-cards summary-cards-compact">
        <div className="summary-card summary-card-empty">
          <span className="summary-card-label">소비 현황</span>
          <p>소비 현황을 불러오지 못했어요.</p>
        </div>
        <AiSummaryBanner
          productBookmarkCount={productBookmarkCount}
          policyBookmarkCount={policyBookmarkCount}
          coachingCount={coachingCount}
        />
      </div>
    );
  }

  if (!summary || summary.setupRequired) {
    return (
      <div className="summary-cards summary-cards-compact">
        <div className="summary-card summary-card-empty">
          <span className="summary-card-icon">
            <DashboardIcon name="sparkle" />
          </span>
          <span className="summary-card-label">소비가이드 설정 필요</span>
          <p>월급과 급여일을 설정하면 오늘 사용 가능 금액을 확인할 수 있어요.</p>
          <Link to="/dashboard/spending" className="summary-card-cta">
            소비가이드 설정하기
          </Link>
        </div>
        <AiSummaryBanner
          productBookmarkCount={productBookmarkCount}
          policyBookmarkCount={policyBookmarkCount}
          coachingCount={coachingCount}
        />
      </div>
    );
  }

  const cycleText = [summary.cycleStartDate, summary.cycleEndDate]
    .filter(Boolean)
    .join(" ~ ");

  return (
    <div className="summary-cards">
      <div className="summary-card summary-card-highlight">
        <span className="summary-card-icon">
          <DashboardIcon name="sparkle" />
        </span>
        <span className="summary-card-label">오늘 사용 가능 금액</span>
        <strong>{formatWon(summary.todayAvailableAmount)}</strong>
        <Link to="/dashboard/spending" className="summary-card-add-link">
          + 소비 직접 입력하기
        </Link>
      </div>

      <div className="summary-card">
        <span className="summary-card-icon">
          <DashboardIcon name="chart" />
        </span>
        <span className="summary-card-label">이번 달 소비</span>
        <strong>{formatWon(summary.todayNetSpend)}</strong>
        <span className="summary-card-sub">
          하루 권장 소비액 {formatWon(summary.dailyRecommendedAmount)}
        </span>
      </div>

      <div className="summary-card">
        <span className="summary-card-icon">
          <DashboardIcon name="check" />
        </span>
        <span className="summary-card-label">이번 주기 남은 예산</span>
        <strong>{formatWon(summary.remainingAmount)}</strong>
        <span className="summary-card-sub">{cycleText || "주기 정보 없음"}</span>
      </div>

      <div className="summary-card">
        <span className="summary-card-icon">
          <DashboardIcon name="building" />
        </span>
        <span className="summary-card-label">월 저축 목표</span>
        <strong>{summary.savingsGoalAmount ? formatWon(summary.savingsGoalAmount) : "저축 목표 미설정"}</strong>
        <span className="summary-card-sub">마이페이지에서 목표를 관리할 수 있어요</span>
      </div>

      <AiSummaryBanner
        productBookmarkCount={productBookmarkCount}
        policyBookmarkCount={policyBookmarkCount}
        coachingCount={coachingCount}
      />
    </div>
  );
}

export default SummaryCards;
