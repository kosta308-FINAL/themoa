import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  dismissCoachingCard,
  getCardConnections,
  getCategories,
  getCategorySummary,
  getCoachingCards,
  getFixedExpenseCandidates,
  getRecentDays,
  getSpendingGuideSummary,
  getTodayTransactions,
  setupSpendingGuide,
  syncCardTransactions,
} from '../../api/spendingGuideApi'
import DashboardIcon from '../../components/common/DashboardIcon'
import DashboardTopNav from '../../components/layout/DashboardTopNav'
import DashboardFooter from '../../components/layout/DashboardFooter'
import BudgetSettingsModal from './BudgetSettingsModal'
import CardManagementModal from './CardManagementModal'
import ManualTransactionModal from './ManualTransactionModal'
import TransactionDetailModal from './TransactionDetailModal'
import '../dashboard/Dashboard.css'
import './SpendingGuidePage.css'

const EMPTY_DATA = {
  summary: null,
  today: null,
  recent: null,
  category: null,
  candidates: null,
  coaching: null,
  categories: null,
  connections: null,
}

const WON = new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 0 })

const toNumber = (value) => Number(value ?? 0)
const formatWon = (value) => `${WON.format(toNumber(value))}원`
const formatDate = (value) => {
  if (!value) return '—'
  const [, month, day] = value.split('-').map(Number)
  return `${month}월 ${day}일`
}
const formatTime = (value) => value?.slice(11, 16) || ''
const todayDate = () => {
  const now = new Date()
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset())
  return now.toISOString().slice(0, 10)
}
const paymentLabel = (transaction) => {
  if (transaction.paymentMethod === 'CASH') return '현금'
  if (transaction.paymentMethod === 'TRANSFER') return '계좌이체'
  return [transaction.cardOrganizationName, transaction.cardNumberMasked].filter(Boolean).join(' · ') || '카드'
}
const transactionAmount = (value) => {
  const amount = toNumber(value)
  return `${amount > 0 ? '-' : amount < 0 ? '+' : ''}${formatWon(Math.abs(amount))}`
}
const errorMessage = (error, fallback) =>
  error?.response?.data?.message || (error?.response?.status === 401 ? '로그인이 필요합니다.' : fallback)

function EmptyState({ icon, title, description }) {
  return (
    <div className="spending-empty">
      <span className="spending-empty-icon"><DashboardIcon name={icon} size={22} /></span>
      <strong>{title}</strong>
      <p>{description}</p>
    </div>
  )
}

function LoadingState({ label = '데이터를 불러오고 있어요.' }) {
  return <div className="spending-loading" role="status"><span className="spending-spinner" />{label}</div>
}

function SectionError({ message }) {
  return <div className="spending-section-error"><DashboardIcon name="info" size={18} />{message}</div>
}

function PanelTitle({ icon, title, description, tone = 'green' }) {
  return (
    <div className="spending-panel-title">
      <span className={`spending-panel-icon ${tone}`}><DashboardIcon name={icon} size={18} /></span>
      <div><h2>{title}</h2><p>{description}</p></div>
    </div>
  )
}

function SetupView({ onComplete }) {
  const [salaryAmount, setSalaryAmount] = useState('')
  const [payday, setPayday] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSalaryChange = (event) => {
    const digits = event.target.value.replace(/\D/g, '').slice(0, 12)
    setSalaryAmount(digits ? WON.format(Number(digits)) : '')
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)
    try {
      await setupSpendingGuide({
        salaryAmount: Number(salaryAmount.replace(/,/g, '')),
        payday: Number(payday),
      })
      await onComplete()
    } catch (requestError) {
      setError(errorMessage(requestError, '소비가이드 설정을 저장하지 못했습니다.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="spending-setup-card">
      <span className="spending-setup-icon"><DashboardIcon name="wallet" size={28} /></span>
      <h2>소비가이드를 시작해 볼까요?</h2>
      <p>월급과 급여일을 기준으로 매일 쓸 수 있는 금액을 계산해드려요.</p>
      <form onSubmit={handleSubmit}>
        <label><span>월 실수령액 *</span><div className="spending-input-suffix"><input inputMode="numeric" value={salaryAmount} onChange={handleSalaryChange} placeholder="0" required /><em>원</em></div></label>
        <label><span>매월 급여일 *</span><select value={payday} onChange={(event) => setPayday(event.target.value)} required><option value="" disabled>급여일 선택</option>{Array.from({ length: 31 }, (_, index) => <option key={index + 1} value={index + 1}>{index + 1}일</option>)}</select></label>
        {error && <div className="spending-form-error"><DashboardIcon name="info" size={16} />{error}</div>}
        <button className="spending-primary" type="submit" disabled={isSubmitting}>{isSubmitting ? '저장 중...' : '소비가이드 시작하기'}</button>
      </form>
    </section>
  )
}

function TodayTransactions({ data, error, onExpand, onSelect }) {
  if (error) return <SectionError message={error} />
  if (!data) return <LoadingState />
  if (!data.items?.length) return <EmptyState icon="receipt" title="아직 표시할 소비내역이 없어요" description="소비내역이 생기면 오늘 거래가 여기에 표시됩니다." />
  return (
    <>
      <div className="spending-transaction-list">
        {data.items.map((transaction) => (
          <button type="button" className="spending-transaction" key={transaction.id} onClick={() => onSelect(transaction.id)}>
            <span className="spending-transaction-icon"><DashboardIcon name={transaction.paymentMethod === 'CARD' ? 'card' : 'receipt'} size={18} /></span>
            <div><strong>{transaction.merchantDisplayName || transaction.merchantNameRaw}</strong><p>{[formatTime(transaction.usedAt), transaction.categoryName, paymentLabel(transaction)].filter(Boolean).join(' · ')}</p></div>
            <span className={toNumber(transaction.netAmount) < 0 ? 'refund' : ''}><strong>{transactionAmount(transaction.netAmount)}</strong><small>{transaction.source === 'MANUAL' ? '직접 입력' : transaction.canceledAmount > 0 ? '취소 반영' : '카드 자동수집'}</small></span>
          </button>
        ))}
      </div>
      {data.items.length < Math.min(8, data.totalCount) && <button type="button" className="spending-list-more" onClick={onExpand}>오늘 내역 {Math.min(8, data.totalCount) - data.items.length}건 더보기</button>}
      {data.items.length >= 8 && data.totalCount > 8 && <Link className="spending-list-more" to={`/dashboard/spending/transactions?date=${todayDate()}`}>오늘 전체 {data.totalCount}건 보기</Link>}
    </>
  )
}

function RecentFlow({ data, error }) {
  const values = data?.days?.map((day) => Math.abs(toNumber(day.netAmount))) || []
  const max = Math.max(toNumber(data?.guideLineAmount), ...values, 1)
  if (error) return <SectionError message={error} />
  if (!data) return <LoadingState />
  if (!data.days?.length || data.days.every((day) => toNumber(day.netAmount) === 0)) return <EmptyState icon="chart" title="소비 흐름을 만들 데이터가 없어요" description="거래 데이터가 쌓이면 최근 7일 소비 흐름을 보여드려요." />
  return (
    <div className="spending-chart">
      <div className="spending-guide-line" style={{ bottom: `${Math.min(100, (toNumber(data.guideLineAmount) / max) * 100)}%` }}><span>권장 {formatWon(data.guideLineAmount)}</span></div>
      <div className="spending-bars">
        {data.days.map((day) => {
          const amount = toNumber(day.netAmount)
          return <Link className="spending-bar-item" to={`/dashboard/spending/transactions?date=${day.date}`} key={day.date}><span className="spending-bar-space"><i className={amount < 0 ? 'negative' : amount > toNumber(data.guideLineAmount) ? 'over' : ''} style={{ height: `${Math.max(4, (Math.abs(amount) / max) * 100)}%` }} /></span><strong>{formatDate(day.date).replace('월 ', '/').replace('일', '')}</strong></Link>
        })}
      </div>
    </div>
  )
}

function CategorySummary({ data, error }) {
  const gradient = useMemo(() => {
    if (!data?.items?.length) return '#edf2ef'
    const colors = ['#22c55e', '#14b8a6', '#60a5fa', '#f59e0b', '#a78bfa', '#f472b6']
    let cursor = 0
    return `conic-gradient(${data.items.map((item, index) => { const start = cursor; cursor += toNumber(item.percentage); return `${colors[index % colors.length]} ${start}% ${cursor}%` }).join(', ')})`
  }, [data])
  if (error) return <SectionError message={error} />
  if (!data) return <LoadingState />
  if (!data.items?.length) return <EmptyState icon="chart" title="분석할 카테고리 데이터가 없어요" description="소비내역이 연결되면 카테고리별 비중이 표시됩니다." />
  return (
    <div className="spending-category-layout">
      <div className="spending-donut" style={{ background: gradient }}><span><strong>{formatWon(data.positiveNetTotal)}</strong><small>순사용액</small></span></div>
      <div className="spending-category-legend">{data.items.map((item, index) => <Link to={`/dashboard/spending/transactions?categoryId=${item.categoryId}`} key={item.categoryId}><i style={{ background: ['#22c55e', '#14b8a6', '#60a5fa', '#f59e0b', '#a78bfa', '#f472b6'][index % 6] }} /><span>{item.categoryName}</span><strong>{formatWon(item.amount)} <small>{toNumber(item.percentage)}%</small></strong></Link>)}</div>
    </div>
  )
}

function FixedCandidates({ data, error }) {
  if (error) return <SectionError message={error} />
  if (!data) return <LoadingState />
  if (!data.length) return <EmptyState icon="repeat" title="아직 발견된 고정지출 후보가 없어요" description="거래가 쌓이면 반복 결제 패턴을 이곳에서 확인할 수 있어요." />
  return <div className="spending-candidate-list">{data.slice(0, 3).map((candidate) => <div key={candidate.id}><span className="spending-candidate-icon"><DashboardIcon name="card" size={17} /></span><div><strong>{candidate.merchantAliasName}</strong><p>매달 약 {formatWon(candidate.avgAmount)} · {candidate.avgPayDay}일쯤</p></div><Link to="/dashboard/fixed-expenses">등록</Link></div>)}</div>
}

function CoachingCards({ data, error, onDismiss, pendingId }) {
  if (error) return <SectionError message={error} />
  if (!data) return <LoadingState />
  if (!data.items?.length) return <EmptyState icon="sparkle" title="아직 제공할 소비 코칭이 없어요" description={data.emptyReason === 'CARD_NOT_CONNECTED' ? '카드를 연결하면 소비 습관을 분석해드려요.' : '분석 가능한 소비내역이 쌓이면 맞춤 코칭을 보여드려요.'} />
  return <div className="spending-coaching-list">{data.items.map((card) => <article key={card.id}><span className="spending-panel-icon purple"><DashboardIcon name="sparkle" size={17} /></span><h3>{card.title}</h3><p>{card.body}</p>{toNumber(card.estimatedSaving) > 0 && <strong>예상 절감액 {formatWon(card.estimatedSaving)}</strong>}<div><button type="button" disabled={pendingId === card.id} onClick={() => onDismiss(card.id, 'NOT_WASTE')}>필요한 소비</button><button type="button" disabled={pendingId === card.id} onClick={() => onDismiss(card.id, 'HIDE')}>그만 보기</button></div></article>)}</div>
}

function SpendingGuidePage() {
  const [data, setData] = useState(EMPTY_DATA)
  const [sectionErrors, setSectionErrors] = useState({})
  const [pageError, setPageError] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [isEntryOpen, setIsEntryOpen] = useState(false)
  const [isBudgetOpen, setIsBudgetOpen] = useState(false)
  const [isCardOpen, setIsCardOpen] = useState(false)
  const [detailId, setDetailId] = useState(null)
  const [editingTransaction, setEditingTransaction] = useState(null)
  const [pendingCoachId, setPendingCoachId] = useState(null)

  const loadGuide = useCallback(async () => {
    setIsLoading(true)
    setPageError('')
    try {
      const summary = await getSpendingGuideSummary()
      if (summary.setupRequired) {
        setData({ ...EMPTY_DATA, summary })
        setSectionErrors({})
        return
      }
      const requests = {
        today: getTodayTransactions(),
        recent: getRecentDays(),
        category: getCategorySummary(),
        candidates: getFixedExpenseCandidates(),
        coaching: getCoachingCards(),
        categories: getCategories(),
        connections: getCardConnections(),
      }
      const entries = Object.entries(requests)
      const results = await Promise.allSettled(entries.map(([, request]) => request))
      const nextData = { ...EMPTY_DATA, summary }
      const nextErrors = {}
      results.forEach((result, index) => {
        const key = entries[index][0]
        if (result.status === 'fulfilled') nextData[key] = result.value
        else nextErrors[key] = errorMessage(result.reason, '데이터를 불러오지 못했습니다.')
      })
      setData(nextData)
      setSectionErrors(nextErrors)
    } catch (error) {
      setPageError(errorMessage(error, '소비가이드를 불러오지 못했습니다.'))
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => { loadGuide() }, [loadGuide])

  const expandToday = async () => {
    try {
      const today = await getTodayTransactions(8)
      setData((current) => ({ ...current, today }))
    } catch (error) {
      setSectionErrors((current) => ({ ...current, today: errorMessage(error, '오늘 거래를 더 불러오지 못했습니다.') }))
    }
  }

  const refreshGuide = async () => {
    setIsLoading(true)
    let syncError = ''
    if (data.connections?.connections?.length && data.connections.cardSyncEnabled) {
      try {
        await syncCardTransactions()
      } catch (error) {
        syncError = errorMessage(error, '카드 소비내역을 새로 불러오지 못했습니다.')
      }
    }
    await loadGuide()
    if (syncError) setPageError(syncError)
  }

  const handleDismiss = async (cardId, dismissType) => {
    if (dismissType === 'HIDE' && !window.confirm('이 코칭 카드를 그만 볼까요?')) return
    setPendingCoachId(cardId)
    try {
      await dismissCoachingCard(cardId, dismissType)
      const coaching = await getCoachingCards()
      setData((current) => ({ ...current, coaching }))
    } catch (error) {
      setSectionErrors((current) => ({ ...current, coaching: errorMessage(error, '코칭 응답을 저장하지 못했습니다.') }))
    } finally {
      setPendingCoachId(null)
    }
  }

  const summary = data.summary
  const dailyRecommended = toNumber(summary?.dailyRecommendedAmount)
  const todaySpent = toNumber(summary?.todayNetSpend)
  const useRate = dailyRecommended > 0 ? Math.max(0, Math.round((todaySpent / dailyRecommended) * 100)) : 0
  const cycleSpent = summary ? toNumber(summary.availableAmount) - toNumber(summary.remainingAmount) : 0

  return (
    <div className="dashboard spending-guide">
      <DashboardTopNav />
      <main className="spending-main">
        <header className="spending-page-head"><div><h1>소비가이드</h1><p>오늘의 기준을 확인하고, 무리 없이 쓸 수 있는 금액을 관리해보세요.</p></div><div className="spending-head-actions"><button type="button" className="spending-refresh" onClick={() => setIsCardOpen(true)}>카드 관리</button><button type="button" className="spending-refresh" onClick={refreshGuide} disabled={isLoading}>새로고침</button></div></header>

        {pageError && <div className="spending-page-error"><DashboardIcon name="info" size={19} /><span>{pageError}</span><button type="button" onClick={loadGuide}>다시 시도</button></div>}
        {isLoading && !summary ? <LoadingState label="소비가이드를 불러오고 있어요." /> : summary?.setupRequired ? <SetupView onComplete={loadGuide} /> : summary && <>
          <section className="spending-hero" aria-label="소비 기준 요약">
            <article className="spending-today-card">
              <div className="spending-card-head"><div><h2>오늘의 소비 기준</h2><p>오늘 하루 동안 권장액은 유지돼요.</p></div><span className={`spending-status ${useRate > 100 ? 'warning' : ''}`}>{useRate > 100 ? '오늘 권장액 초과' : '안정적으로 사용 중'}</span></div>
              <div className="spending-number-grid"><div className="spending-number-main"><span><DashboardIcon name="wallet" size={16} />오늘 사용 가능 금액</span><strong>{formatWon(summary.todayAvailableAmount)}</strong><p>{useRate > 100 ? `오늘 권장액을 ${formatWon(todaySpent - dailyRecommended)} 초과했어요.` : '오늘 남은 권장 금액이에요.'}</p></div><div className="spending-mini-stat"><span>하루 권장 소비액</span><strong>{formatWon(summary.dailyRecommendedAmount)}</strong><p>자정까지 고정</p></div><div className="spending-mini-stat"><span>오늘 순사용액</span><strong>{formatWon(summary.todayNetSpend)}</strong><p>취소 반영 금액</p></div></div>
              <div className="spending-progress-meta"><span>오늘 권장액 사용률</span><strong>{useRate}%</strong></div><div className="spending-progress"><i style={{ width: `${Math.min(100, Math.max(0, useRate))}%` }} /></div>
            </article>
            <aside className="spending-cycle-card"><div className="spending-cycle-top"><span>이번 급여 주기</span><button type="button" onClick={() => setIsBudgetOpen(true)}>예산 기준</button></div><h2>남은 예산</h2><strong className={summary.overCycleBudget ? 'spending-cycle-amount negative' : 'spending-cycle-amount'}>{formatWon(summary.remainingAmount)}</strong><p>{formatDate(summary.cycleStartDate)} ~ {formatDate(summary.cycleEndDate)}</p><div className="spending-cycle-bottom"><div><span>남은 기간</span><strong>{summary.remainingDays}일</strong></div><div><span>주기 순사용액</span><strong>{formatWon(cycleSpent)}</strong></div></div></aside>
          </section>

          <div className="spending-content-grid">
            <div className="spending-column">
              <section className="spending-panel"><div className="spending-panel-head"><PanelTitle icon="receipt" title="오늘 거래" description="고정지출을 제외한 오늘의 거래를 보여드려요" /><button type="button" className="spending-secondary" onClick={() => setIsEntryOpen(true)} disabled={!data.categories?.length}><DashboardIcon name="plus" size={15} />지출 직접 입력</button></div><TodayTransactions data={data.today} error={sectionErrors.today} onExpand={expandToday} onSelect={setDetailId} /><div className="spending-panel-footer"><Link to="/dashboard/spending/transactions">전체 소비내역 보기</Link></div></section>
              <section className="spending-panel spending-flow-panel"><div className="spending-panel-head"><PanelTitle icon="chart" title="최근 7일 소비 흐름" description="날짜별 순사용액과 하루 권장액을 비교해요" tone="blue" /></div><RecentFlow data={data.recent} error={sectionErrors.recent} /></section>
            </div>
            <div className="spending-column">
              <section className="spending-panel spending-category-panel"><div className="spending-panel-head"><PanelTitle icon="chart" title="카테고리별 소비" description="실제 소비 순액을 기준으로 보여드려요" tone="teal" /></div><CategorySummary data={data.category} error={sectionErrors.category} /><div className="spending-panel-footer"><Link to="/dashboard/spending/transactions">카테고리 상세보기</Link></div></section>
              <section className="spending-panel"><div className="spending-panel-head"><PanelTitle icon="repeat" title="고정지출 후보" description="반복되는 결제를 찾아 알려드려요" tone="orange" />{data.candidates?.length > 3 && <Link className="spending-panel-link" to="/dashboard/fixed-expenses">나머지 {data.candidates.length - 3}개</Link>}</div><FixedCandidates data={data.candidates} error={sectionErrors.candidates} /></section>
              <section className="spending-panel"><div className="spending-panel-head"><PanelTitle icon="sparkle" title="이번 달 이렇게 아껴봐요" description="지난 소비 습관을 바탕으로 알려드려요" tone="purple" /></div><CoachingCards data={data.coaching} error={sectionErrors.coaching} onDismiss={handleDismiss} pendingId={pendingCoachId} /></section>
            </div>
          </div>
        </>}
      </main>
      <DashboardFooter />
      {isEntryOpen && <ManualTransactionModal categories={data.categories} allowCard={Boolean(data.connections && (!data.connections.connections?.length || !data.connections.cardSyncEnabled))} onClose={() => setIsEntryOpen(false)} onSaved={loadGuide} />}
      {isBudgetOpen && summary && <BudgetSettingsModal summary={summary} onClose={() => setIsBudgetOpen(false)} onSaved={loadGuide} />}
      {isCardOpen && <CardManagementModal onClose={() => setIsCardOpen(false)} onChanged={loadGuide} />}
      {detailId && <TransactionDetailModal transactionId={detailId} categories={data.categories} onClose={() => setDetailId(null)} onChanged={loadGuide} onEdit={(transaction) => { setDetailId(null); setEditingTransaction(transaction) }} />}
      {editingTransaction && <ManualTransactionModal transaction={editingTransaction} categories={data.categories} allowCard={Boolean(data.connections && (!data.connections.connections?.length || !data.connections.cardSyncEnabled))} onClose={() => setEditingTransaction(null)} onSaved={loadGuide} />}
    </div>
  )
}

export default SpendingGuidePage
