import { Link } from 'react-router-dom'
import DashboardIcon from '../../../components/common/DashboardIcon'
import DashboardTopNav from '../../../components/layout/DashboardTopNav'
import DashboardFooter from '../../../components/layout/DashboardFooter'
import PolicyAdminJobControls from './components/PolicyAdminJobControls'
import PolicyAdminJobProgress from './components/PolicyAdminJobProgress'
import PolicyAdminStatusPanel from './components/PolicyAdminStatusPanel'
import PolicyEmbeddingStatus from './components/PolicyEmbeddingStatus'
import { usePolicyAdmin } from './hooks/usePolicyAdmin'
import './PolicyAdminPage.css'

function PolicyAdminPage() {
  const admin = usePolicyAdmin()

  return (
    <div className="policy-admin-page">
      <DashboardTopNav />
      <main className="policy-admin-main">
        <section className="policy-admin-header">
          <div>
            <p className="policy-admin-eyebrow">Local policy operations</p>
            <h1>정책 데이터 관리</h1>
            <p>로컬 개발 환경에서 youth-test 원본 정책/RAG 작업을 실행하고 상태를 확인합니다.</p>
          </div>
          <Link className="policy-admin-back" to="/dashboard/policy">
            일반 정책 검색으로 돌아가기
            <DashboardIcon name="chevron-right" size={16} />
          </Link>
        </section>

        {admin.dashboardError && <div className="policy-admin-alert">{admin.dashboardError}</div>}
        {admin.notice && <div className="policy-admin-alert">{admin.notice}</div>}
        {admin.pollingError && <div className="policy-admin-alert">작업 상태 갱신 실패: {admin.pollingError}</div>}

        <PolicyAdminStatusPanel dashboard={admin.dashboard} job={admin.job} />
        <PolicyAdminJobControls running={admin.running} busyKey={admin.busyKey} onStart={admin.runJob} />
        <PolicyAdminJobProgress job={admin.job} onRefreshDashboard={admin.refreshDashboard} />
        <PolicyEmbeddingStatus
          embeddings={admin.embeddings}
          embeddingStatus={admin.embeddingStatus}
          onStatusChange={admin.setEmbeddingStatus}
          embeddingKeyword={admin.embeddingKeyword}
          onKeywordChange={admin.setEmbeddingKeyword}
          embeddingPage={admin.embeddingPage}
          onRefreshEmbeddings={admin.refreshEmbeddings}
        />
      </main>
      <DashboardFooter />
    </div>
  )
}

export default PolicyAdminPage
