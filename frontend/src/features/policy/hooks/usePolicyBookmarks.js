import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  addPolicyBookmark,
  deletePolicyBookmark,
  getPolicyBookmarks,
} from '../../../api/policyApi'
import { getApiErrorMessage } from '../../../utils/apiError'

export const usePolicyBookmarks = () => {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busyPolicyId, setBusyPolicyId] = useState(null)

  const bookmarkedPolicyIds = useMemo(
    () => new Set(items.map((item) => item.policyId)),
    [items],
  )

  const loadBookmarks = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const data = await getPolicyBookmarks()
      setItems(data?.items || [])
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, '즐겨찾기 정보를 불러오지 못했어요.'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      loadBookmarks()
    }, 0)
    return () => window.clearTimeout(timer)
  }, [loadBookmarks])

  const isBookmarked = useCallback(
    (policyId) => bookmarkedPolicyIds.has(policyId),
    [bookmarkedPolicyIds],
  )

  const addBookmark = useCallback(async (policyId) => {
    if (busyPolicyId === policyId) return null
    setBusyPolicyId(policyId)
    setError('')
    try {
      const bookmark = await addPolicyBookmark(policyId)
      setItems((current) => {
        const withoutDuplicate = current.filter((item) => item.policyId !== bookmark.policyId)
        return [bookmark, ...withoutDuplicate]
      })
      return bookmark
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, '정책을 즐겨찾기에 추가하지 못했어요.'))
      return null
    } finally {
      setBusyPolicyId(null)
    }
  }, [busyPolicyId])

  const removeBookmark = useCallback(async (policyId) => {
    if (busyPolicyId === policyId) return false
    setBusyPolicyId(policyId)
    setError('')
    try {
      await deletePolicyBookmark(policyId)
      setItems((current) => current.filter((item) => item.policyId !== policyId))
      return true
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, '즐겨찾기를 해제하지 못했어요.'))
      return false
    } finally {
      setBusyPolicyId(null)
    }
  }, [busyPolicyId])

  const toggleBookmark = useCallback((policyId) => (
    bookmarkedPolicyIds.has(policyId) ? removeBookmark(policyId) : addBookmark(policyId)
  ), [addBookmark, bookmarkedPolicyIds, removeBookmark])

  return {
    items,
    loading,
    error,
    busyPolicyId,
    loadBookmarks,
    isBookmarked,
    addBookmark,
    removeBookmark,
    toggleBookmark,
  }
}
