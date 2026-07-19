const dash = (value) => value ?? '-'
const numberText = (value) => Number(value ?? 0).toLocaleString('ko-KR')

function PolicyAdminJobProgress({ job, onRefreshDashboard }) {
  const progress = job?.overallProgressPercent ?? job?.stageProgressPercent ?? 0

  return (
    <section className="policy-admin-panel">
      <div className="policy-admin-section-title">
        <h2>작업 진행 상태</h2>
        <button type="button" onClick={onRefreshDashboard}>새로고침</button>
      </div>
      {job ? (
        <div className="policy-admin-job">
          <div className="policy-admin-progress"><span style={{ width: `${Math.min(100, Math.max(0, progress))}%` }} /></div>
          <dl>
            <div><dt>jobType</dt><dd>{dash(job.jobType)}</dd></div>
            <div><dt>status</dt><dd>{dash(job.status)}</dd></div>
            <div><dt>현재 단계</dt><dd>{dash(job.stageLabel || job.stage)}</dd></div>
            <div><dt>현재 페이지</dt><dd>{dash(job.currentPage)} / {dash(job.totalPages)}</dd></div>
            <div><dt>처리 건수</dt><dd>{numberText(job.processedCount)} / {numberText(job.totalCount)}</dd></div>
            <div><dt>성공/실패</dt><dd>{numberText(job.successCount)} / {numberText(job.failedCount)}</dd></div>
            <div><dt>진행률</dt><dd>{progress}%</dd></div>
            <div><dt>경과 시간</dt><dd>{numberText(job.elapsedTimeMs)} ms</dd></div>
            <div><dt>예상 남은 시간</dt><dd>{dash(job.estimatedRemainingSeconds)}</dd></div>
            <div><dt>오류 메시지</dt><dd>{dash(job.message)}</dd></div>
          </dl>
        </div>
      ) : (
        <div className="policy-admin-empty">최근 작업이 없습니다.</div>
      )}
    </section>
  )
}

export default PolicyAdminJobProgress
