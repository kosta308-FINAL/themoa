import { useEffect, useMemo, useRef, useState } from 'react'
import { getPolicyDetail, searchPolicies } from '../../../api/policyApi'

const POLICY_PAGE_SIZE = 10
const POLICY_SEARCH_RESULT_LIMIT = 50

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

  const allResults = useMemo(() => result?.results || [], [result])
  const totalPages = useMemo(() => Math.ceil(allResults.length / POLICY_PAGE_SIZE), [allResults.length])
  const results = useMemo(() => {
    const start = page * POLICY_PAGE_SIZE
    return allResults.slice(start, start + POLICY_PAGE_SIZE)
  }, [allResults, page])
  const hasPreviousPage = page > 0
  const hasNextPage = page + 1 < totalPages
  const totalText = useMemo(() => {
    if (!result) return '검색 전'
    return `${result.totalMatched ?? allResults.length}건`
  }, [allResults.length, result])

  const runSearch = async () => {
    if (!query.trim()) {
      setError('검색어를 입력하세요.')
      return
    }
    detailRequestIdRef.current += 1
    setPage(0)
    setLoading(true)
    setError('')
    setSelected(null)
    setDetailLoading(false)
    try {
      const data = await searchPolicies({
        query: query.trim(),
        resultSize: POLICY_SEARCH_RESULT_LIMIT,
        page: 0,
        size: POLICY_SEARCH_RESULT_LIMIT,
      })
      setResult(data)
    } catch (searchError) {
      setResult(null)
      setPage(0)
      setError(errorMessage(searchError))
    } finally {
      setLoading(false)
    }
  }

  const changePage = (nextPage) => {
    if (nextPage < 0 || nextPage >= totalPages) {
      return
    }

    detailRequestIdRef.current += 1
    setPage(nextPage)
    setSelected(null)
    setDetailLoading(false)
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
    totalPages,
    hasNextPage,
    hasPreviousPage,
    runSearch,
    changePage,
    openDetail,
  }
}
