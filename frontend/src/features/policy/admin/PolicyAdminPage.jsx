import AdminLayout from '../../../components/layout/AdminLayout'
import PolicyAdminJobControls from './components/PolicyAdminJobControls'
import PolicyAdminJobProgress from './components/PolicyAdminJobProgress'
import PolicyAdminStatusPanel from './components/PolicyAdminStatusPanel'
import PolicyEmbeddingStatus from './components/PolicyEmbeddingStatus'
import { usePolicyAdmin } from './hooks/usePolicyAdmin'
import './PolicyAdminPage.css'

function PolicyAdminPage() {
  const admin = usePolicyAdmin()

  return (
    <AdminLayout
      title="정책 데이터 관리"
      subtitle="정책 수집, 동기화, 검색 인덱스 및 임베딩 상태를 관리합니다"
    >
      <div className="policy-admin-page">
        <div className="policy-admin-main">

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
        </div>
      </div>
    </AdminLayout>
  )
}

export default PolicyAdminPage
