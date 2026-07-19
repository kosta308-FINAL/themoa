const policyJobs = [
  ['policy-collection', '정책 API 수집 실행'],
  ['search-projection-rebuild', 'Search Projection 생성'],
  ['search-index-refresh', '검색 인덱스 갱신'],
  ['embedding-queue', 'Embedding 대기열 등록'],
  ['embedding-process', 'Embedding 처리 실행'],
  ['embedding-retry-failed', '실패 Embedding 재시도'],
  ['full-reindex', '전체 재색인'],
]

const regionJobs = [
  ['region-catalog-sync', '전국 행정지역 동기화'],
  ['policy-region-rebuild', '정책 지역 다시 계산'],
  ['region-catalog-repair', '지역 카탈로그 복구'],
]

function JobButtonList({ jobs, running, busyKey, onStart }) {
  return (
    <div className="policy-admin-buttons">
      {jobs.map(([key, label]) => (
        <button key={key} type="button" disabled={running || !!busyKey} onClick={() => onStart(key, label)}>
          {busyKey === key ? '요청 중' : label}
        </button>
      ))}
    </div>
  )
}

function PolicyAdminJobControls({ running, busyKey, onStart }) {
  return (
    <section className="policy-admin-columns">
      <div className="policy-admin-panel">
        <h2>정책·인덱싱 관리</h2>
        <JobButtonList jobs={policyJobs} running={running} busyKey={busyKey} onStart={onStart} />
      </div>
      <div className="policy-admin-panel">
        <h2>지역 관리</h2>
        <JobButtonList jobs={regionJobs} running={running} busyKey={busyKey} onStart={onStart} />
      </div>
    </section>
  )
}

export default PolicyAdminJobControls
