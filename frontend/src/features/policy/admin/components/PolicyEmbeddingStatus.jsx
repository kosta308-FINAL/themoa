const dash = (value) => value ?? '-'

function PolicyEmbeddingStatus({
  embeddings,
  embeddingStatus,
  onStatusChange,
  embeddingKeyword,
  onKeywordChange,
  embeddingPage,
  onRefreshEmbeddings,
}) {
  return (
    <section className="policy-admin-panel">
      <div className="policy-admin-section-title">
        <h2>Embedding 처리 내역</h2>
        <button type="button" onClick={() => onRefreshEmbeddings(0)}>조회</button>
      </div>
      <div className="policy-admin-filters">
        <select value={embeddingStatus} onChange={(event) => onStatusChange(event.target.value)}>
          <option value="">전체 상태</option>
          <option value="PENDING">PENDING</option>
          <option value="PROCESSING">PROCESSING</option>
          <option value="SYNCED">SYNCED</option>
          <option value="FAILED">FAILED</option>
        </select>
        <input
          value={embeddingKeyword}
          onChange={(event) => onKeywordChange(event.target.value)}
          placeholder="정책명 또는 ID"
        />
      </div>
      <div className="policy-admin-table-wrap">
        <table>
          <thead>
            <tr>
              <th>정책명</th>
              <th>정책 ID</th>
              <th>상태</th>
              <th>마지막 갱신</th>
              <th>실패 사유</th>
            </tr>
          </thead>
          <tbody>
            {(embeddings?.items || []).map((item) => (
              <tr key={item.embeddingSyncId}>
                <td>{dash(item.policyTitle)}</td>
                <td>{dash(item.policyId || item.sourcePolicyId)}</td>
                <td>{dash(item.syncStatus)}</td>
                <td>{dash(item.syncedAt || item.requestedAt)}</td>
                <td>{dash(item.lastError)}</td>
              </tr>
            ))}
            {(!embeddings?.items || embeddings.items.length === 0) && (
              <tr><td colSpan="5">Embedding 내역이 없습니다.</td></tr>
            )}
          </tbody>
        </table>
      </div>
      <div className="policy-admin-pager">
        <button type="button" disabled={embeddingPage <= 0} onClick={() => onRefreshEmbeddings(embeddingPage - 1)}>이전</button>
        <span>{embeddingPage + 1} / {Math.max(1, embeddings?.totalPages || 1)}</span>
        <button type="button" disabled={!embeddings?.hasNext} onClick={() => onRefreshEmbeddings(embeddingPage + 1)}>다음</button>
      </div>
    </section>
  )
}

export default PolicyEmbeddingStatus
