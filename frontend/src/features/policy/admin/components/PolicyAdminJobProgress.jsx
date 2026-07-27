const dash = (value) => value ?? '-'
const numberText = (value) => Number(value ?? 0).toLocaleString('ko-KR')
const runningStatuses = new Set(['RUNNING', 'STARTED', 'PROCESSING', 'PENDING'])

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleString('ko-KR')
}

const staleStatus = (job) => {
  if (!job?.updatedAt || !runningStatuses.has(job.status)) return false
  const updatedAt = new Date(job.updatedAt)
  if (Number.isNaN(updatedAt.getTime())) return false
  return Date.now() - updatedAt.getTime() > 2 * 60 * 1000
}

function PolicyAdminJobProgress({ job, onRefreshDashboard }) {
  const progress = job?.overallProgressPercent ?? job?.stageProgressPercent ?? 0
  const stalled = staleStatus(job)

  return (
    <section className="policy-admin-panel">
      <div className="policy-admin-section-title">
        <h2>작업 진행 상태</h2>
        <button type="button" onClick={onRefreshDashboard}>새로고침</button>
      </div>
      {job ? (
        <div className="policy-admin-job">
          <div className="policy-admin-progress"><span style={{ width: `${Math.min(100, Math.max(0, progress))}%` }} /></div>
          {stalled && (
            <div className="policy-admin-alert">
              작업 상태가 일정 시간 동안 갱신되지 않았습니다. 백엔드 작업 로그와 외부 API 응답 상태를 확인해주세요.
            </div>
          )}
          <dl>
            <div><dt>jobType</dt><dd>{dash(job.jobType)}</dd></div>
            <div><dt>status</dt><dd>{dash(job.status)}</dd></div>
            <div><dt>현재 단계</dt><dd>{dash(job.stageLabel || job.stage)}</dd></div>
            <div><dt>현재 페이지</dt><dd>{dash(job.currentPage)} / {dash(job.totalPages)}</dd></div>
            <div><dt>현재 Batch</dt><dd>{dash(job.currentBatch)} / {dash(job.totalBatches)}</dd></div>
            <div><dt>처리 건수</dt><dd>{numberText(job.processedCount)} / {numberText(job.totalCount)}</dd></div>
            <div><dt>성공/실패/건너뜀</dt><dd>{numberText(job.successCount)} / {numberText(job.failedCount)} / {numberText(job.skippedCount)}</dd></div>
            <div><dt>API 요청/재시도</dt><dd>{numberText(job.apiRequestCount)} / {numberText(job.retryCount)}</dd></div>
            <div><dt>진행률</dt><dd>{progress}%</dd></div>
            <div><dt>경과 시간</dt><dd>{numberText(job.elapsedTimeMs)} ms</dd></div>
            <div><dt>예상 남은 시간</dt><dd>{dash(job.estimatedRemainingSeconds)}</dd></div>
            <div><dt>최근 갱신</dt><dd>{formatDateTime(job.updatedAt)}</dd></div>
            <div><dt>완료 시각</dt><dd>{formatDateTime(job.completedAt)}</dd></div>
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
