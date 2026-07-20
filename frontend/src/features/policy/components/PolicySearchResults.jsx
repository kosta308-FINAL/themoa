import PolicyResultCard from './PolicyResultCard'

function PolicySearchResults({ loading, result, results, selected, page, onSearch, onOpenDetail }) {
  return (
    <div className="policy-results">
      {loading && <div className="policy-empty">검색 중입니다.</div>}
      {!loading && result && results.length === 0 && <div className="policy-empty">조건에 맞는 정책이 없습니다.</div>}
      {!loading && !result && <div className="policy-empty">검색어를 입력해 정책을 찾아보세요.</div>}
      {results.map((item) => (
        <PolicyResultCard
          key={`${item.policyId}-${item.sourcePolicyId}`}
          item={item}
          active={selected?.policyId === item.policyId}
          onOpen={onOpenDetail}
        />
      ))}
      {result && (
        <div className="policy-pagination">
          <button type="button" disabled={page <= 0 || loading} onClick={() => onSearch(page - 1)}>
            이전
          </button>
          <span>{page + 1}</span>
          <button type="button" disabled={!result.hasNext || loading} onClick={() => onSearch(page + 1)}>
            다음
          </button>
        </div>
      )}
    </div>
  )
}

export default PolicySearchResults
