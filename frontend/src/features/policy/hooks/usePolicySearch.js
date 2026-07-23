import { useEffect, useMemo, useRef, useState } from 'react'
import { getPolicyDetail, searchPolicies } from '../../../api/policyApi'

const errorMessage = (error) => {
  const code = error?.response?.data?.code
  if (code === 'POLICY_SEARCH_NOT_READY') {
    return '정책 검색 데이터가 아직 준비되지 않았습니다. 정책 데이터 관리에서 수집과 인덱싱을 먼저 실행하세요.'
  }
  return error?.response?.data?.message || '정책 검색 요청을 처리하지 못했습니다.'
}

export const usePolicySearch = (initialQuery) => {
  const [query, setQuery] = useState(initialQuery)
  const [page, setPage] = useState(0)
  const [result, setResult] = useState(null)
  const [selected, setSelected] = useState(null)
  const [loading, setLoading] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [error, setError] = useState('')
  const detailRequestIdRef = useRef(0)

  const results = result?.results || []
  const totalText = useMemo(() => {
    if (!result) return '검색 전'
    return `${result.totalMatched ?? results.length}건`
  }, [result, results.length])

  const runSearch = async (nextPage = 0) => {
    if (!query.trim()) {
      setError('검색어를 입력하세요.')
      return
    }
    detailRequestIdRef.current += 1
    setLoading(true)
    setError('')
    setSelected(null)
    setDetailLoading(false)
    try {
      const data = await searchPolicies({ query: query.trim(), page: nextPage, size: 10 })
      setResult(data)
      setPage(nextPage)
    } catch (searchError) {
      setResult(null)
      setError(errorMessage(searchError))
    } finally {
      setLoading(false)
    }
  }

  const openDetail = async (policyId) => {
    const requestId = ++detailRequestIdRef.current
    setDetailLoading(true)
    setError('')
    try {
      const detail = await getPolicyDetail(policyId)
      if (requestId !== detailRequestIdRef.current) return
      setSelected(detail)
    } catch (detailError) {
      if (requestId !== detailRequestIdRef.current) return
      setError(detailError?.response?.data?.message || '정책 상세 정보를 불러오지 못했습니다.')
    } finally {
      if (requestId === detailRequestIdRef.current) {
        setDetailLoading(false)
      }
    }
  }

  useEffect(
    () => () => {
      detailRequestIdRef.current += 1
    },
    [],
  )

  return {
    query,
    setQuery,
    page,
    result,
    results,
    selected,
    loading,
    detailLoading,
    error,
    totalText,
    runSearch,
    openDetail,
  }
}
