const dash = (value) => value ?? '-'
const numberText = (value) => Number(value ?? 0).toLocaleString('ko-KR')

const field = (dashboard, key) => {
  const status = dashboard?.status
  const readiness = dashboard?.readiness
  const searchIndex = dashboard?.searchIndex
  return status?.[key] ?? readiness?.[key] ?? searchIndex?.[key]
}

function StatusCard({ label, value }) {
  return (
    <div className="policy-admin-stat">
      <span>{label}</span>
      <strong>{dash(value)}</strong>
    </div>
  )
}

function PolicyAdminStatusPanel({ dashboard, job }) {
  return (
    <section className="policy-admin-grid">
      <StatusCard label="전체 정책 수" value={numberText(field(dashboard, 'totalPolicyCount'))} />
      <StatusCard label="활성 정책 수" value={numberText(field(dashboard, 'activePolicyCount'))} />
      <StatusCard label="지역 코드 수" value={numberText(field(dashboard, 'regionTotalCount'))} />
      <StatusCard label="Search Projection 수" value={numberText(field(dashboard, 'projectionCount'))} />
      <StatusCard label="검색 Index 문서 수" value={numberText(field(dashboard, 'lexicalIndexDocumentCount'))} />
      <StatusCard label="Embedding 완료" value={numberText(field(dashboard, 'syncedCount') ?? field(dashboard, 'syncedEmbeddingCount'))} />
      <StatusCard label="Embedding 대기" value={numberText(field(dashboard, 'pendingCount'))} />
      <StatusCard label="Embedding 처리 중" value={numberText(field(dashboard, 'processingCount'))} />
      <StatusCard label="Embedding 실패" value={numberText(field(dashboard, 'failedCount'))} />
      <StatusCard label="검색 준비 여부" value={field(dashboard, 'ready') ? 'READY' : 'NOT READY'} />
      <StatusCard label="마지막 지역 동기화" value={dash(field(dashboard, 'lastRegionSyncTime'))} />
      <StatusCard label="현재 실행 Job" value={dash(job?.jobType || dashboard?.currentJob?.jobType)} />
    </section>
  )
}

export default PolicyAdminStatusPanel
