import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getCardConnections, getCategories, getSpendingTransactions } from '../../api/spendingGuideApi'
import DashboardIcon from '../../components/common/DashboardIcon'
import DashboardTopNav from '../../components/layout/DashboardTopNav'
import DashboardFooter from '../../components/layout/DashboardFooter'
import ManualTransactionModal from './ManualTransactionModal'
import TransactionDetailModal from './TransactionDetailModal'
import '../dashboard/Dashboard.css'
import './SpendingGuidePage.css'

const WON = new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 0 })
const toNumber = (value) => Number(value ?? 0)
const formatWon = (value) => `${WON.format(Math.abs(toNumber(value)))}원`
const formatDate = (value) => value
  ? new Intl.DateTimeFormat('ko-KR', { dateStyle: 'long' }).format(new Date(`${value}T00:00:00`))
  : '—'
const formatDateTime = (value) => value
  ? new Intl.DateTimeFormat('ko-KR', { timeStyle: 'short' }).format(new Date(value))
  : '—'

function SpendingHistoryPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [items, setItems] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [detailId, setDetailId] = useState(null)
  const [editingTransaction, setEditingTransaction] = useState(null)
  const [isEntryOpen, setIsEntryOpen] = useState(false)
  const [categories, setCategories] = useState([])
  const [connections, setConnections] = useState(null)
  const [refreshKey, setRefreshKey] = useState(0)
  const queryKey = searchParams.toString()

  useEffect(() => {
    Promise.allSettled([getCategories(), getCardConnections()]).then(([categoryResult, connectionResult]) => {
      if (categoryResult.status === 'fulfilled') setCategories(categoryResult.value || [])
      if (connectionResult.status === 'fulfilled') setConnections(connectionResult.value)
    })
  }, [])

  useEffect(() => {
    let active = true
    const load = async () => {
      setIsLoading(true)
      setError('')
      const params = Object.fromEntries(new URLSearchParams(queryKey).entries())
      try {
        const response = await getSpendingTransactions({ ...params, page, size: 20 })
        if (!active) return
        setItems((current) => page === 0 ? response.items : [...current, ...response.items])
        setTotalPages(response.totalPages)
        setTotalElements(response.totalElements)
      } catch (requestError) {
        if (active) setError(requestError.response?.data?.message || '소비내역을 불러오지 못했습니다.')
      } finally {
        if (active) setIsLoading(false)
      }
    }
    load()
    return () => { active = false }
  }, [page, queryKey, refreshKey])

  const reload = useCallback(async () => {
    setItems([])
    if (page !== 0) setPage(0)
    else setRefreshKey((current) => current + 1)
  }, [page])

  const categoryId = searchParams.get('categoryId')
  const selectedCategory = categories.find((category) => String(category.id) === categoryId)
  const selectedDate = searchParams.get('date')
  const groups = useMemo(() => items.reduce((result, transaction) => {
    const date = transaction.usedDate || transaction.usedAt?.slice(0, 10) || '날짜 미상'
    const last = result[result.length - 1]
    if (last?.date === date) last.items.push(transaction)
    else result.push({ date, items: [transaction] })
    return result
  }, []), [items])
  const allowCard = Boolean(connections && (!connections.connections?.length || !connections.cardSyncEnabled))

  const clearFilters = () => {
    setItems([])
    setPage(0)
    setSearchParams({})
  }

  return (
    <div className="dashboard spending-guide">
      <DashboardTopNav />
      <main className="spending-main">
        <Link className="spending-back-link" to="/dashboard/spending">← 소비가이드로 돌아가기</Link>
        <header className="spending-page-head"><div><h1>전체 소비내역</h1><p>현재 급여 주기의 소비내역을 최신순으로 확인하세요.</p></div><button type="button" className="spending-secondary" onClick={() => setIsEntryOpen(true)} disabled={!categories.length}><DashboardIcon name="plus" size={15} />지출 직접 입력</button></header>
        {(selectedDate || categoryId) && <div className="spending-filter-bar"><span>적용된 필터</span>{selectedDate && <strong>{formatDate(selectedDate)}</strong>}{categoryId && <strong>{selectedCategory?.name || `카테고리 ${categoryId}`}</strong>}<button type="button" onClick={clearFilters}>필터 해제</button></div>}
        <section className="spending-panel spending-history-panel">
          {!error && <div className="spending-history-summary">총 {WON.format(totalElements)}건</div>}
          {error && <div className="spending-section-error"><DashboardIcon name="info" size={18} />{error}<button type="button" onClick={reload}>다시 시도</button></div>}
          {!error && !items.length && isLoading && <div className="spending-loading"><span className="spending-spinner" />소비내역을 불러오는 중...</div>}
          {!error && !items.length && !isLoading && <div className="spending-empty"><span className="spending-empty-icon"><DashboardIcon name="receipt" size={22} /></span><strong>{selectedDate || categoryId ? '조건에 맞는 소비내역이 없어요' : '이 기간엔 소비 기록이 없어요'}</strong><p>직접 기록하거나 카드 내역을 연결하면 이곳에서 확인할 수 있어요.</p>{selectedDate || categoryId ? <button type="button" className="spending-secondary" onClick={clearFilters}>필터 해제</button> : <button type="button" className="spending-secondary" onClick={() => setIsEntryOpen(true)} disabled={!categories.length}>지출 직접 입력</button>}</div>}
          <div className="spending-history-groups">{groups.map((group) => <section key={group.date}><h2>{group.date === '날짜 미상' ? group.date : formatDate(group.date)}</h2><div className="spending-history-list">{group.items.map((transaction) => {
            const amount = toNumber(transaction.netAmount)
            return <button type="button" key={transaction.id} onClick={() => setDetailId(transaction.id)}><span className="spending-transaction-icon"><DashboardIcon name={transaction.paymentMethod === 'CARD' ? 'card' : 'receipt'} size={18} /></span><div><strong>{transaction.merchantDisplayName || transaction.merchantNameRaw}</strong><p>{formatDateTime(transaction.usedAt)} · {transaction.categoryName} · {transaction.paymentMethod === 'CASH' ? '현금' : transaction.paymentMethod === 'TRANSFER' ? '계좌이체' : transaction.cardOrganizationName || '카드'}</p></div><span className={amount < 0 ? 'refund' : ''}><strong>{amount > 0 ? '-' : amount < 0 ? '+' : ''}{formatWon(amount)}</strong><small>{transaction.source === 'MANUAL' ? '직접 입력' : transaction.canceledAmount > 0 ? '취소 반영' : '카드 자동수집'}</small></span></button>
          })}</div></section>)}</div>
          {page + 1 < totalPages && <button type="button" className="spending-history-more" onClick={() => setPage((current) => current + 1)} disabled={isLoading}>{isLoading ? '불러오는 중...' : '더 보기'}</button>}
        </section>
      </main>
      <DashboardFooter />
      {detailId && <TransactionDetailModal transactionId={detailId} categories={categories} onClose={() => setDetailId(null)} onChanged={reload} onEdit={(transaction) => { setDetailId(null); setEditingTransaction(transaction) }} />}
      {isEntryOpen && <ManualTransactionModal categories={categories} allowCard={allowCard} onClose={() => setIsEntryOpen(false)} onSaved={reload} />}
      {editingTransaction && <ManualTransactionModal transaction={editingTransaction} categories={categories} allowCard={allowCard} onClose={() => setEditingTransaction(null)} onSaved={reload} />}
    </div>
  )
}

export default SpendingHistoryPage
