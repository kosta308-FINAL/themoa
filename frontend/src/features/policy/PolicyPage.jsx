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

function PolicyPage() {
  const search = usePolicySearch('')

  return (
    <main className="dash-main policy-page">
      <div className="dash-topbar policy-topbar">
        <div>
          <h1>정책 지원</h1>
          <p>
            지역, 나이, 학생 여부와 취업 상태를 자연어로 입력해 내 조건과 관련된 정책을 찾아보세요.
          </p>
        </div>
      </div>

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
  )
}

export default PolicyPage
