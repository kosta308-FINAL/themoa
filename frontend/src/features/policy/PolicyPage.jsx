import { Link } from 'react-router-dom'
import DashboardIcon from '../../components/common/DashboardIcon'
import PolicyDetailPanel from './components/PolicyDetailPanel'
import PolicySearchForm from './components/PolicySearchForm'
import PolicySearchResults from './components/PolicySearchResults'
import PolicySearchStatus from './components/PolicySearchStatus'
import { usePolicySearch } from './hooks/usePolicySearch'
import './PolicyPage.css'

const examples = [
  '수원에 사는 27살 취업 준비생이 받을 수 있는 정책',
  '서울에 사는 대학생이 받을 수 있는 지원 정책',
  '대구에 사는 22살 청년이 받을 수 있는 금융 지원',
  '경기도에 사는 무직 청년이 신청할 수 있는 정책',
]

const policyLocalToolsEnabled =
  import.meta.env.DEV && import.meta.env.VITE_POLICY_LOCAL_TOOLS_ENABLED === 'true'

function PolicyPage() {
  const search = usePolicySearch(examples[0])

  return (
    <div className="policy-page">
      <main className="policy-main">
        <section className="policy-header">
          <div>
            <p className="policy-eyebrow">Youth policy search</p>
            <h1>정책 지원</h1>
            <p className="policy-description">
              지역, 나이, 학생 여부, 취업 상태를 자연어로 입력하면 준비된 정책/RAG 인덱스에서 조건에 맞는 정책만 찾습니다.
            </p>
          </div>
          {policyLocalToolsEnabled && (
            <Link className="policy-admin-link" to="/dashboard/policy/admin">
              정책 데이터 관리
              <DashboardIcon name="chevron-right" size={16} />
            </Link>
          )}
        </section>

        <PolicySearchForm
          query={search.query}
          examples={examples}
          loading={search.loading}
          totalText={search.totalText}
          onQueryChange={search.setQuery}
          onSearch={search.runSearch}
        />

        {search.error && <div className="policy-alert">{search.error}</div>}
        <PolicySearchStatus result={search.result} />

        <section className="policy-content">
          <PolicySearchResults
            loading={search.loading}
            result={search.result}
            results={search.results}
            selected={search.selected}
            page={search.page}
            onSearch={search.runSearch}
            onOpenDetail={search.openDetail}
          />
          <PolicyDetailPanel selected={search.selected} detailLoading={search.detailLoading} />
        </section>
      </main>
    </div>
  )
}

export default PolicyPage
