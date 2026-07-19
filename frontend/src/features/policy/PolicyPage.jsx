import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import DashboardIcon from '../../components/common/DashboardIcon'
import DashboardTopNav from '../../components/layout/DashboardTopNav'
import DashboardFooter from '../../components/layout/DashboardFooter'
import { getPolicyDetail, searchPolicies } from '../../api/policyApi'
import '../dashboard/Dashboard.css'
import './PolicyPage.css'

const examples = [
  '수원에 사는 27살 취업 준비생이 받을 수 있는 정책',
  '서울에 사는 대학생이 받을 수 있는 지원 정책',
  '대구에 사는 22살 청년이 받을 수 있는 금융 지원',
  '경기도에 사는 무직 청년이 신청할 수 있는 정책',
]

const dash = (value) => value || '-'
const listText = (value) => Array.isArray(value) && value.length ? value.join(', ') : '-'

const errorMessage = (error) => {
  const code = error?.response?.data?.code
  if (code === 'POLICY_SEARCH_NOT_READY') {
    return '정책 검색 데이터가 아직 준비되지 않았습니다. 정책 데이터 관리에서 수집과 인덱싱을 먼저 실행하세요.'
  }
  return error?.response?.data?.message || '정책 검색 요청을 처리하지 못했습니다.'
}

function PolicyPage() {
  const [query, setQuery] = useState(examples[0])
  const [page, setPage] = useState(0)
  const [result, setResult] = useState(null)
  const [selected, setSelected] = useState(null)
  const [loading, setLoading] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [error, setError] = useState('')

  const results = result?.results || []
  const condition = result?.interpretedCondition
  const totalText = useMemo(() => {
    if (!result) return '검색 전'
    return `${result.totalMatched ?? results.length}건`
  }, [result, results.length])

  const runSearch = async (nextPage = 0) => {
    if (!query.trim()) {
      setError('검색어를 입력하세요.')
      return
    }
    setLoading(true)
    setError('')
    setSelected(null)
    try {
      const data = await searchPolicies({ query: query.trim(), page: nextPage, size: 10 })
      setResult(data)
      setPage(nextPage)
    } catch (searchError) {
      setResult(null)
      setError(errorMessage(searchError))
    } finally {
      setLoading(false)
    }
  }

  const openDetail = async (policyId) => {
    setDetailLoading(true)
    setError('')
    try {
      setSelected(await getPolicyDetail(policyId))
    } catch (detailError) {
      setError(detailError?.response?.data?.message || '정책 상세 정보를 불러오지 못했습니다.')
    } finally {
      setDetailLoading(false)
    }
  }

  return (
    <div className="dashboard policy-page">
      <DashboardTopNav />
      <main className="policy-main">
        <section className="policy-header">
          <div>
            <p className="policy-eyebrow">Youth policy search</p>
            <h1>정책 지원</h1>
            <p className="policy-description">
              지역, 나이, 학생 여부, 취업 상태를 자연어로 입력하면 준비된 정책/RAG 인덱스에서 조건에 맞는 정책만 찾습니다.
            </p>
          </div>
          {import.meta.env.DEV && (
            <Link className="policy-admin-link" to="/dashboard/policy/admin">
              정책 데이터 관리
              <DashboardIcon name="chevron-right" size={16} />
            </Link>
          )}
        </section>

        <section className="policy-search-panel">
          <textarea
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            rows={3}
            placeholder="예: 수원에 사는 27살 취업 준비생이 받을 수 있는 정책"
          />
          <div className="policy-example-row">
            {examples.map((example) => (
              <button key={example} type="button" onClick={() => setQuery(example)}>
                {example}
              </button>
            ))}
          </div>
          <div className="policy-search-actions">
            <button type="button" className="policy-primary-button" disabled={loading} onClick={() => runSearch(0)}>
              <DashboardIcon name="search" size={17} />
              {loading ? '검색 중' : '정책 찾기'}
            </button>
            <span>{totalText}</span>
          </div>
        </section>

        {error && <div className="policy-alert">{error}</div>}

        {condition && (
          <section className="policy-condition-strip">
            <span>지역: {dash([condition.province, condition.city, condition.district].filter(Boolean).join(' '))}</span>
            <span>나이: {dash(condition.age || condition.inferredAge)}</span>
            <span>취업: {dash(condition.employmentStatus)}</span>
            <span>학생: {condition.studentStatus ? '해당' : '-'}</span>
            <span>검색 모드: {dash(result.searchMode)}</span>
          </section>
        )}

        <section className="policy-content">
          <div className="policy-results">
            {loading && <div className="policy-empty">검색 중입니다.</div>}
            {!loading && result && results.length === 0 && <div className="policy-empty">조건에 맞는 정책이 없습니다.</div>}
            {!loading && !result && !error && <div className="policy-empty">검색어를 입력해 정책을 찾아보세요.</div>}
            {results.map((item) => (
              <button
                key={`${item.policyId}-${item.sourcePolicyId}`}
                type="button"
                className={`policy-result-card${selected?.policyId === item.policyId ? ' active' : ''}`}
                onClick={() => openDetail(item.policyId)}
              >
                <div className="policy-result-title-row">
                  <strong>{item.title}</strong>
                  <span>{dash(item.applicationStatus)}</span>
                </div>
                <p>{dash(item.summary)}</p>
                <div className="policy-meta-row">
                  <span>{dash(item.region)}</span>
                  <span>{dash(item.agencyName)}</span>
                  <span>{dash(item.category)}</span>
                </div>
                <div className="policy-meta-row">
                  <span>연령 {dash(item.minAge)} - {dash(item.maxAge)}</span>
                  <span>{dash(item.regionMatchLabel || item.regionCompatibility)}</span>
                </div>
              </button>
            ))}
            {result && (
              <div className="policy-pagination">
                <button type="button" disabled={page <= 0 || loading} onClick={() => runSearch(page - 1)}>
                  이전
                </button>
                <span>{page + 1}</span>
                <button type="button" disabled={!result.hasNext || loading} onClick={() => runSearch(page + 1)}>
                  다음
                </button>
              </div>
            )}
          </div>

          <aside className="policy-detail-panel">
            {detailLoading && <div className="policy-empty">상세 정보를 불러오는 중입니다.</div>}
            {!detailLoading && !selected && <div className="policy-empty">결과 카드를 선택하면 상세 정보가 표시됩니다.</div>}
            {!detailLoading && selected && (
              <>
                <p className="policy-eyebrow">{dash(selected.sourcePolicyId)}</p>
                <h2>{selected.title}</h2>
                <dl className="policy-detail-list">
                  <div><dt>기관</dt><dd>{dash(selected.agencyName)}</dd></div>
                  <div><dt>분야</dt><dd>{dash(selected.category)}</dd></div>
                  <div><dt>상태</dt><dd>{dash(selected.status)}</dd></div>
                  <div><dt>지역</dt><dd>{listText(selected.regions)}</dd></div>
                  <div><dt>요약</dt><dd>{dash(selected.summary)}</dd></div>
                </dl>
                {selected.officialUrl && (
                  <a className="policy-official-link" href={selected.officialUrl} target="_blank" rel="noreferrer">
                    공식 링크
                    <DashboardIcon name="chevron-right" size={16} />
                  </a>
                )}
              </>
            )}
          </aside>
        </section>
      </main>
      <DashboardFooter />
    </div>
  )
}

export default PolicyPage
