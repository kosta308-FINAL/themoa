import DashboardIcon from '../../../components/common/DashboardIcon'
import { usePolicyBookmarks } from '../hooks/usePolicyBookmarks'

const dash = (value) => value || '-'
const listText = (value) => (Array.isArray(value) && value.length ? value.join(', ') : '-')

function PolicyDetailPanel({ selected, detailLoading }) {
  const bookmarks = usePolicyBookmarks()
  const selectedPolicyId = selected?.policyId
  const bookmarked = selectedPolicyId ? bookmarks.isBookmarked(selectedPolicyId) : false
  const bookmarkBusy = selectedPolicyId != null && bookmarks.busyPolicyId === selectedPolicyId
  const bookmarkDisabled = bookmarks.loading || bookmarkBusy
  const bookmarkLabel = bookmarkBusy
    ? '처리 중...'
    : bookmarks.loading
      ? '즐겨찾기 확인 중...'
      : bookmarked
        ? '즐겨찾기 해제'
        : '즐겨찾기 추가'

  const handleToggleBookmark = () => {
    if (selectedPolicyId == null) return
    bookmarks.toggleBookmark(selectedPolicyId)
  }

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
          <div className="policy-detail-actions">
            <button
              className={`policy-bookmark-button${bookmarked ? ' active' : ''}`}
              type="button"
              disabled={bookmarkDisabled}
              onClick={handleToggleBookmark}
            >
              {bookmarkLabel}
            </button>
            {selected.officialUrl && (
              <a className="policy-official-link" href={selected.officialUrl} target="_blank" rel="noreferrer">
                공식 링크
                <DashboardIcon name="chevron-right" size={16} />
              </a>
            )}
          </div>
          {bookmarks.error && <p className="policy-bookmark-error">{bookmarks.error}</p>}
        </>
      )}
    </aside>
  )
}

export default PolicyDetailPanel
