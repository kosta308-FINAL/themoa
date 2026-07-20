import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  getPolicyAdminDashboard,
  getPolicyEmbeddingHistory,
  getPolicyJob,
  getPolicyLatestJob,
  startPolicyJob,
} from '../../../../api/policyApi'

const runningStatuses = new Set(['RUNNING', 'STARTED', 'PROCESSING', 'PENDING'])
const terminalStatuses = new Set(['COMPLETED', 'COMPLETED_WITH_ERRORS', 'FAILED', 'CANCELED', 'CANCELLED', 'BLOCKED', 'NOT_READY'])

const apiError = (error) => error?.response?.data?.message || error?.message || '요청을 처리하지 못했습니다.'

export const usePolicyAdmin = () => {
  const [dashboard, setDashboard] = useState(null)
  const [dashboardError, setDashboardError] = useState('')
  const [job, setJob] = useState(null)
  const [busyKey, setBusyKey] = useState('')
  const [embeddings, setEmbeddings] = useState(null)
  const [embeddingStatus, setEmbeddingStatus] = useState('')
  const [embeddingKeyword, setEmbeddingKeyword] = useState('')
  const [embeddingPage, setEmbeddingPage] = useState(0)
  const [notice, setNotice] = useState('')
  const [pollingError, setPollingError] = useState('')

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

  const refreshLatestJob = useCallback(async () => {
    try {
      const latest = await getPolicyLatestJob()
      if (latest) {
        setJob(latest)
      }
      setPollingError('')
    } catch (error) {
      setPollingError(apiError(error))
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
      refreshLatestJob()
      refreshEmbeddings(0)
    }, 0)
    return () => window.clearTimeout(timer)
  }, [refreshDashboard, refreshEmbeddings, refreshLatestJob])

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
        setPollingError('')
        if (terminalStatuses.has(nextJob.status)) {
          setBusyKey('')
        }
      } catch (error) {
        setPollingError(apiError(error))
        setBusyKey('')
      }
    }, 1500)
    return () => window.clearInterval(timer)
  }, [job?.jobId, job?.status, refreshDashboard, refreshEmbeddings])

  const runJob = async (jobKey, label) => {
    if (!window.confirm(`${label} 작업을 실행할까요?`)) return
    setBusyKey(jobKey)
    setNotice('')
    setPollingError('')
    try {
      setJob(await startPolicyJob(jobKey))
    } catch (error) {
      setNotice(apiError(error))
      setBusyKey('')
    }
  }

  return {
    dashboard,
    dashboardError,
    job,
    busyKey,
    embeddings,
    embeddingStatus,
    setEmbeddingStatus,
    embeddingKeyword,
    setEmbeddingKeyword,
    embeddingPage,
    notice,
    pollingError,
    running,
    refreshDashboard,
    refreshEmbeddings,
    runJob,
  }
}
