import DashboardIcon from '../../../components/common/DashboardIcon'

function PolicySearchForm({ query, examples, loading, totalText, onQueryChange, onSearch }) {
  return (
    <section className="policy-search-panel">
      <textarea
        value={query}
        onChange={(event) => onQueryChange(event.target.value)}
        rows={3}
        placeholder="예: 수원에 사는 27살 취업 준비생이 받을 수 있는 정책"
      />
      <div className="policy-example-row">
        {examples.map((example) => (
          <button key={example} type="button" onClick={() => onQueryChange(example)}>
            {example}
          </button>
        ))}
      </div>
      <div className="policy-search-actions">
        <button type="button" className="policy-primary-button" disabled={loading} onClick={() => onSearch(0)}>
          <DashboardIcon name="search" size={17} />
          {loading ? '검색 중' : '정책 찾기'}
        </button>
        <span>{totalText}</span>
      </div>
    </section>
  )
}

export default PolicySearchForm
