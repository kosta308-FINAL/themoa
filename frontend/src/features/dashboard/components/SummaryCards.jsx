import { Link } from 'react-router-dom'
import DashboardIcon from '../../../components/common/DashboardIcon'
import AiSummaryBanner from './AiSummaryBanner'
import { mockFinancialInfo } from '../../../constants/mockDashboard'

function InfoPrompt({ label, icon }) {
  return (
    <div className="summary-card summary-card-empty">
      <span className="summary-card-icon"><DashboardIcon name={icon} /></span>
      <span className="summary-card-label">{label}</span>
      <p>정보를 입력하면 확인할 수 있어요</p>
      <button type="button" className="summary-card-cta">정보 입력하기</button>
    </div>
  )
}

function SummaryCards() {
  const info = mockFinancialInfo

  if (!info.hasFinancialInfo) {
    return (
      <div className="summary-cards">
        <InfoPrompt label="오늘 사용 가능 금액" icon="sparkle" />
        <InfoPrompt label="이번 달 소비" icon="chart" />
        <InfoPrompt label="목표 달성률" icon="check" />
        <InfoPrompt label="신용점수" icon="building" />
        <AiSummaryBanner />
      </div>
    )
  }

  return (
    <div className="summary-cards">
      <div className="summary-card summary-card-highlight">
        <span className="summary-card-icon"><DashboardIcon name="sparkle" /></span>
        <span className="summary-card-label">오늘 사용 가능 금액</span>
        <strong>{info.todayAvailable}</strong>
        <Link to="/dashboard/spending" className="summary-card-add-link">
          + 소비 직접 입력하기
        </Link>
      </div>

      <div className="summary-card">
        <span className="summary-card-icon"><DashboardIcon name="chart" /></span>
        <span className="summary-card-label">이번 달 소비</span>
        <strong>{info.monthSpending}</strong>
        <span className="summary-card-badge summary-card-badge-down">{info.monthSpendingDiff}</span>
      </div>

      <div className="summary-card">
        <span className="summary-card-icon"><DashboardIcon name="check" /></span>
        <span className="summary-card-label">목표 달성률</span>
        <strong>{info.goalRate}%</strong>
        <div className="summary-card-progress">
          <div className="summary-card-progress-fill" style={{ width: `${info.goalRate}%` }} />
        </div>
        <span className="summary-card-sub">{info.goalDetail}</span>
      </div>

      <div className="summary-card">
        <span className="summary-card-icon"><DashboardIcon name="building" /></span>
        <span className="summary-card-label">신용점수</span>
        <strong>{info.creditScore}점</strong>
        <span className="summary-card-badge summary-card-badge-up">{info.creditPercentile}</span>
      </div>

      <AiSummaryBanner />
    </div>
  )
}

export default SummaryCards
