import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import DashboardIcon from '../../../components/common/DashboardIcon'
import DashboardTopNav from '../../../components/layout/DashboardTopNav'
import DashboardFooter from '../../../components/layout/DashboardFooter'
import {
  getPolicyAdminDashboard,
  getPolicyEmbeddingHistory,
  getPolicyJob,
  getPolicyLatestJob,
  startPolicyJob,
} from '../../../api/policyApi'
import '../../dashboard/Dashboard.css'
import './PolicyAdminPage.css'

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

const runningStatuses = new Set(['RUNNING', 'STARTED', 'PROCESSING', 'PENDING'])
const terminalStatuses = new Set(['COMPLETED', 'COMPLETED_WITH_ERRORS', 'FAILED', 'CANCELED', 'CANCELLED', 'BLOCKED', 'NOT_READY'])

const dash = (value) => value ?? '-'
const numberText = (value) => Number(value ?? 0).toLocaleString('ko-KR')

const field = (dashboard, key) => {
  const status = dashboard?.status
  const readiness = dashboard?.readiness
  const searchIndex = dashboard?.searchIndex
  return status?.[key] ?? readiness?.[key] ?? searchIndex?.[key]
}

const apiError = (error) => error?.response?.data?.message || error?.message || '요청을 처리하지 못했습니다.'

function StatusCard({ label, value }) {
  return (
    <div className="policy-admin-stat">
      <span>{label}</span>
      <strong>{dash(value)}</strong>
    </div>
  )
}

function PolicyAdminPage() {
  const [dashboard, setDashboard] = useState(null)
  const [dashboardError, setDashboardError] = useState('')
  const [job, setJob] = useState(null)
  const [busyKey, setBusyKey] = useState('')
  const [embeddings, setEmbeddings] = useState(null)
  const [embeddingStatus, setEmbeddingStatus] = useState('')
  const [embeddingKeyword, setEmbeddingKeyword] = useState('')
  const [embeddingPage, setEmbeddingPage] = useState(0)
  const [notice, setNotice] = useState('')

  const running = useMemo(() => job && runningStatuses.has(job.status), [job])

  const refreshDashboard = useCallback(async () => {
    try {
      const data = await getPolicyAdminDashboard()
      setDashboard(data)
      setDashboardError('')
      if (data?.currentJob) {
        setJob(data.currentJob)
      }
    } catch (error) {
      setDashboardError(apiError(error))
    }
  }, [])

  const refreshEmbeddings = useCallback(async (nextPage = embeddingPage) => {
    try {
      setEmbeddings(await getPolicyEmbeddingHistory({
        status: embeddingStatus,
        keyword: embeddingKeyword,
        page: nextPage,
        size: 20,
      }))
      setEmbeddingPage(nextPage)
    } catch (error) {
      setNotice(apiError(error))
    }
  }, [embeddingKeyword, embeddingPage, embeddingStatus])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      refreshDashboard()
      getPolicyLatestJob().then((latest) => {
        if (latest) setJob(latest)
      }).catch(() => {})
      refreshEmbeddings(0)
    }, 0)
    return () => window.clearTimeout(timer)
  }, [refreshDashboard, refreshEmbeddings])

  useEffect(() => {
    if (!job?.jobId || terminalStatuses.has(job.status)) {
      if (job?.status && terminalStatuses.has(job.status)) {
        const timer = window.setTimeout(() => {
          refreshDashboard()
          refreshEmbeddings()
        }, 0)
        return () => window.clearTimeout(timer)
      }
      return undefined
    }
    const timer = window.setInterval(async () => {
      try {
        const nextJob = await getPolicyJob(job.jobId)
        setJob(nextJob)
        if (terminalStatuses.has(nextJob.status)) {
          setBusyKey('')
        }
      } catch (error) {
        setNotice(apiError(error))
        setBusyKey('')
      }
    }, 1500)
    return () => window.clearInterval(timer)
  }, [job?.jobId, job?.status, refreshDashboard, refreshEmbeddings])

  const startJob = async (jobKey, label) => {
    if (!window.confirm(`${label} 작업을 실행할까요?`)) return
    setBusyKey(jobKey)
    setNotice('')
    try {
      setJob(await startPolicyJob(jobKey))
    } catch (error) {
      setNotice(apiError(error))
      setBusyKey('')
    }
  }

  const progress = job?.overallProgressPercent ?? job?.stageProgressPercent ?? 0

  return (
    <div className="dashboard policy-admin-page">
      <DashboardTopNav />
      <main className="policy-admin-main">
        <section className="policy-admin-header">
          <div>
            <p className="policy-admin-eyebrow">Local policy operations</p>
            <h1>정책 데이터 관리</h1>
            <p>로컬 개발 환경에서 youth-test 원본 정책/RAG 작업을 실행하고 상태를 확인합니다.</p>
          </div>
          <Link className="policy-admin-back" to="/dashboard/policy">
            일반 정책 검색으로 돌아가기
            <DashboardIcon name="chevron-right" size={16} />
          </Link>
        </section>

        {dashboardError && <div className="policy-admin-alert">{dashboardError}</div>}
        {notice && <div className="policy-admin-alert">{notice}</div>}

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

        <section className="policy-admin-columns">
          <div className="policy-admin-panel">
            <h2>정책·인덱싱 관리</h2>
            <div className="policy-admin-buttons">
              {policyJobs.map(([key, label]) => (
                <button key={key} type="button" disabled={running || !!busyKey} onClick={() => startJob(key, label)}>
                  {busyKey === key ? '요청 중' : label}
                </button>
              ))}
            </div>
          </div>
          <div className="policy-admin-panel">
            <h2>지역 관리</h2>
            <div className="policy-admin-buttons">
              {regionJobs.map(([key, label]) => (
                <button key={key} type="button" disabled={running || !!busyKey} onClick={() => startJob(key, label)}>
                  {busyKey === key ? '요청 중' : label}
                </button>
              ))}
            </div>
          </div>
        </section>

        <section className="policy-admin-panel">
          <div className="policy-admin-section-title">
            <h2>작업 진행 상태</h2>
            <button type="button" onClick={refreshDashboard}>새로고침</button>
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

        <section className="policy-admin-panel">
          <div className="policy-admin-section-title">
            <h2>Embedding 처리 내역</h2>
            <button type="button" onClick={() => refreshEmbeddings(0)}>조회</button>
          </div>
          <div className="policy-admin-filters">
            <select value={embeddingStatus} onChange={(event) => setEmbeddingStatus(event.target.value)}>
              <option value="">전체 상태</option>
              <option value="PENDING">PENDING</option>
              <option value="PROCESSING">PROCESSING</option>
              <option value="SYNCED">SYNCED</option>
              <option value="FAILED">FAILED</option>
            </select>
            <input
              value={embeddingKeyword}
              onChange={(event) => setEmbeddingKeyword(event.target.value)}
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
            <button type="button" disabled={embeddingPage <= 0} onClick={() => refreshEmbeddings(embeddingPage - 1)}>이전</button>
            <span>{embeddingPage + 1} / {Math.max(1, embeddings?.totalPages || 1)}</span>
            <button type="button" disabled={!embeddings?.hasNext} onClick={() => refreshEmbeddings(embeddingPage + 1)}>다음</button>
          </div>
        </section>
      </main>
      <DashboardFooter />
    </div>
  )
}

export default PolicyAdminPage
