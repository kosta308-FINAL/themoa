import DashboardIcon from '../../../components/common/DashboardIcon'

const dash = (value) => value || '-'
const listText = (value) => (Array.isArray(value) && value.length ? value.join(', ') : '-')

function PolicyDetailPanel({ selected, detailLoading }) {
  return (
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
  )
}

export default PolicyDetailPanel
