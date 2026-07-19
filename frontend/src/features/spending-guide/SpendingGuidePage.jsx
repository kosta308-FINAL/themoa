import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  dismissCoachingCard,
  createCardConnection,
  getCardConnections,
  getCardIssuers,
  getCategories,
  getCategorySummary,
  getCoachingCards,
  getFixedExpenseCandidates,
  getInitialSyncStatus,
  getRecentDays,
  getSpendingGuideSummary,
  getTodayTransactions,
  retryInitialSync,
  setupSpendingGuide,
} from '../../api/spendingGuideApi'
import DashboardIcon from '../../components/common/DashboardIcon'
import DashboardTopNav from '../../components/layout/DashboardTopNav'
import DashboardFooter from '../../components/layout/DashboardFooter'
import BudgetSettingsModal from './BudgetSettingsModal'
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
const INITIAL_SYNC_IN_PROGRESS = new Set(['NOT_STARTED', 'FETCHING', 'ANALYZING'])

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

function InitialSyncLoading({ compact = false, title, description }) {
  return <div className={`spending-initial-loading${compact ? ' compact' : ''}`} role="status"><span className="spending-spinner" /><strong>{title}</strong><p>{description}</p></div>
}

function InitialSyncView({ summary, syncState, retryingId, onRetry }) {
  const failed = syncState?.overallStatus === 'FAILED'
  const failedConnections = syncState?.connections?.filter((connection) => connection.initialSyncStatus === 'FAILED') || []
  const analyzing = syncState?.overallStatus === 'ANALYZING'
  const loadingDescription = analyzing ? '불러온 거래를 정리하고 분석하는 중이에요.' : '소비내역 수집이 끝나면 자동으로 표시돼요.'

  return <>
    {failed && <div className="spending-page-error spending-sync-error"><DashboardIcon name="info" size={19} /><span>카드 소비내역을 불러오지 못했어요. 다시 수집을 눌러 재시도해주세요.</span>{failedConnections.map((connection) => <button type="button" key={connection.connectionId} disabled={retryingId === connection.connectionId} onClick={() => onRetry(connection.connectionId)}>{retryingId === connection.connectionId ? '재시도 중...' : `${connection.organizationName} 다시 수집`}</button>)}</div>}
    <section className="spending-hero spending-initial-sync" aria-label="카드 소비내역 초기 수집 상태">
      <article className="spending-today-card">
        <div className="spending-card-head"><div><h2>오늘의 소비 기준</h2><p>오늘 하루 동안 권장액은 유지돼요.</p></div><span className={`spending-status${failed ? ' warning' : ''}`}>{failed ? '수집 확인 필요' : '불러오는 중'}</span></div>
        <InitialSyncLoading title={failed ? '소비 기준 계산을 기다리고 있어요' : '오늘 소비 기준을 계산하고 있어요'} description={failed ? '카드 내역을 다시 수집하면 자동으로 계산돼요.' : loadingDescription} />
      </article>
      <aside className="spending-cycle-card">
        <span>이번 급여 주기</span>
        <p>{formatDate(summary?.cycleStartDate)} ~ {formatDate(summary?.cycleEndDate)}</p>
        <InitialSyncLoading compact title="남은 예산을 계산하고 있어요" description="소비내역 수집이 끝나면 자동으로 표시돼요." />
      </aside>
    </section>
    <div className="spending-content-grid spending-initial-sync-grid">
      <div className="spending-column">
        <section className="spending-panel"><div className="spending-panel-head"><PanelTitle icon="receipt" title="오늘 거래" description="고정지출을 제외한 오늘의 거래를 보여드려요" /></div><InitialSyncLoading title="최근 소비내역을 불러오고 있어요" description="화면을 나가도 수집은 계속 진행돼요." /></section>
        <section className="spending-panel"><div className="spending-panel-head"><PanelTitle icon="chart" title="최근 7일 소비 흐름" description="날짜별 순사용액과 하루 권장액을 비교해요" tone="blue" /></div><InitialSyncLoading title="최근 소비 흐름을 만들고 있어요" description="날짜별 순사용액을 정리하는 중이에요." /></section>
      </div>
      <div className="spending-column">
        <section className="spending-panel"><div className="spending-panel-head"><PanelTitle icon="chart" title="카테고리별 소비" description="실제 소비 순액을 기준으로 보여드려요" tone="teal" /></div><InitialSyncLoading title="카테고리를 분석하고 있어요" description="불러온 거래를 카테고리별로 정리하는 중이에요." /></section>
        <section className="spending-panel"><div className="spending-panel-head"><PanelTitle icon="sparkle" title="이번 달 이렇게 아껴봐요" description="지난 소비 습관을 바탕으로 알려드려요" tone="purple" /></div><InitialSyncLoading title="소비 습관을 분석하고 있어요" description="분석할 내역이 충분한지 함께 확인하고 있어요." /></section>
      </div>
    </div>
  </>
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

function SetupView({ onComplete, onCardConnected }) {
  const [step, setStep] = useState(1)
  const [salaryAmount, setSalaryAmount] = useState('')
  const [payday, setPayday] = useState('')
  const [isPaydayOpen, setIsPaydayOpen] = useState(false)
  const [issuers, setIssuers] = useState([])
  const [isIssuerLoading, setIsIssuerLoading] = useState(false)
  const [showBirthDate, setShowBirthDate] = useState(false)
  const [cardForm, setCardForm] = useState({ organization: '', loginId: '', loginPassword: '', cardNo: '', cardPassword: '', birthDate: '' })
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const selectedIssuer = useMemo(
    () => issuers.find((issuer) => issuer.organization === cardForm.organization),
    [cardForm.organization, issuers],
  )

  const handleSalaryChange = (event) => {
    const digits = event.target.value.replace(/\D/g, '').slice(0, 12)
    setSalaryAmount(digits ? WON.format(Number(digits)) : '')
  }

  const handleSalarySubmit = async (event) => {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)
    try {
      await setupSpendingGuide({
        salaryAmount: Number(salaryAmount.replace(/,/g, '')),
        payday: Number(payday),
      })
      setStep(2)
    } catch (requestError) {
      setError(errorMessage(requestError, '소비가이드 설정을 저장하지 못했습니다.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  const openCardStep = async () => {
    setStep(3)
    setError('')
    if (issuers.length) return
    setIsIssuerLoading(true)
    try {
      setIssuers((await getCardIssuers()) || [])
    } catch (requestError) {
      setError(errorMessage(requestError, '지원 카드사를 불러오지 못했습니다.'))
    } finally {
      setIsIssuerLoading(false)
    }
  }

  const updateCardForm = (key) => (event) => {
    setCardForm((current) => ({ ...current, [key]: event.target.value }))
    setError('')
  }

  const handleCardSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)
    try {
      const connection = await createCardConnection({
        organization: cardForm.organization,
        loginId: cardForm.loginId,
        loginPassword: cardForm.loginPassword,
        cardNo: cardForm.cardNo || null,
        cardPassword: cardForm.cardPassword || null,
        birthDate: cardForm.birthDate || null,
      })
      await onCardConnected(connection)
    } catch (requestError) {
      if (requestError.response?.data?.code === 'CARD_CONNECTION_BIRTHDATE_REQUIRED') {
        setShowBirthDate(true)
      }
      setError(errorMessage(requestError, '카드를 연결하지 못했습니다. 입력 정보를 확인해주세요.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="spending-setup-shell">
      <div className="spending-setup-progress" aria-label={`설정 ${step}/3단계`}>
        {[1, 2, 3].map((number) => <span className={number <= step ? 'active' : ''} key={number} />)}
      </div>

      {step === 1 && <>
        <div className="spending-setup-copy">
          <span className="spending-setup-icon"><DashboardIcon name="wallet" size={28} /></span>
          <h2>소비 가이드를 시작해 볼까요?</h2>
          <p>월급과 급여일을 기준으로 매일 쓸 수 있는 금액을 계산해드려요.</p>
        </div>
        <form className="spending-setup-form" onSubmit={handleSalarySubmit}>
          <label><span>월 실수령액 *</span><div className="spending-input-suffix"><input inputMode="numeric" value={salaryAmount} onChange={handleSalaryChange} placeholder="0" required /><em>원</em></div></label>
          <label className="spending-payday-field"><span>매월 급여일 *</span><button className="spending-payday-trigger" type="button" aria-expanded={isPaydayOpen} onClick={() => setIsPaydayOpen((open) => !open)}><strong>{payday ? `${payday}일` : '급여일 선택'}</strong><DashboardIcon name="calendar" size={18} /></button>{isPaydayOpen && <div className="spending-payday-picker"><strong>매월 급여일 선택</strong><div>{Array.from({ length: 31 }, (_, index) => { const day = index + 1; return <button className={Number(payday) === day ? 'selected' : ''} type="button" key={day} onClick={() => { setPayday(String(day)); setIsPaydayOpen(false) }}>{day}</button> })}</div></div>}</label>
          <div className="spending-setup-wide">
            <p className="spending-setup-help">29~31일이 없는 달에는 마지막 날을 기준으로 계산해요. 주말이나 공휴일로 실제 입금일이 달라져도 회사가 정한 급여일을 입력해주세요.</p>
            <div className="spending-setup-notice"><span><DashboardIcon name="info" size={17} /></span>저축 목표는 나중에 설정할 수 있으며, 미설정 시 0원으로 계산돼요.</div>
            {error && <div className="spending-form-error"><DashboardIcon name="info" size={16} />{error}</div>}
            <button className="spending-setup-action" type="submit" disabled={isSubmitting || !salaryAmount || !payday}>{isSubmitting ? '저장 중...' : '다음'}</button>
          </div>
        </form>
      </>}

      {step === 2 && <>
        <div className="spending-setup-copy">
          <span className="spending-setup-icon"><DashboardIcon name="receipt" size={28} /></span>
          <h2>소비내역을 어떻게 기록할까요?</h2>
          <p>카드를 연결해도 현금과 계좌이체 지출은 언제든 직접 기록할 수 있어요.</p>
        </div>
        <div className="spending-method-cards">
          <button type="button" onClick={openCardStep}><span><DashboardIcon name="card" size={24} /></span><strong>카드 내역 자동으로 불러오기</strong><p>카드 결제내역과 카테고리를 자동으로 정리해드려요.</p></button>
          <button type="button" onClick={onComplete}><span><DashboardIcon name="receipt" size={24} /></span><strong>직접 입력해서 시작하기</strong><p>카드 연결 없이 현금·카드·계좌이체 내역을 직접 기록할 수 있어요.</p></button>
        </div>
      </>}

      {step === 3 && <>
        <div className="spending-setup-copy">
          <span className="spending-setup-icon"><DashboardIcon name="card" size={28} /></span>
          <h2>사용 중인 카드를 연결해주세요</h2>
          <p>카드사 계정 하나를 연결하면 해당 계정의 카드를 모두 불러와요.</p>
        </div>
        <form className="spending-setup-card-form" onSubmit={handleCardSubmit}>
          <label><span>카드사 *</span><select value={cardForm.organization} onChange={updateCardForm('organization')} disabled={isIssuerLoading} required><option value="" disabled>{isIssuerLoading ? '카드사를 불러오는 중...' : '카드사를 선택해주세요'}</option>{issuers.map((issuer) => <option value={issuer.organization} key={issuer.organization}>{issuer.name}</option>)}</select></label>
          <label><span>카드사 로그인 아이디 *</span><input value={cardForm.loginId} onChange={updateCardForm('loginId')} autoComplete="username" placeholder="카드사 홈페이지 아이디" required /></label>
          <label><span>카드사 로그인 비밀번호 *</span><input type="password" value={cardForm.loginPassword} onChange={updateCardForm('loginPassword')} autoComplete="current-password" placeholder="카드사 홈페이지 비밀번호" required /></label>
          {selectedIssuer?.requiresCardCredentials && <><label><span>카드번호 *</span><input value={cardForm.cardNo} onChange={updateCardForm('cardNo')} inputMode="numeric" placeholder="카드번호" required /></label><label><span>카드 비밀번호 앞 2자리 *</span><input type="password" value={cardForm.cardPassword} onChange={updateCardForm('cardPassword')} inputMode="numeric" maxLength={2} placeholder="앞 2자리" required /></label></>}
          {showBirthDate && <label><span>생년월일(주민등록번호) *</span><input value={cardForm.birthDate} onChange={updateCardForm('birthDate')} inputMode="numeric" placeholder="본인 확인을 위해 추가 정보가 필요해요" required /></label>}
          <div className="spending-setup-notice"><span><DashboardIcon name="info" size={17} /></span>입력한 카드사 아이디와 비밀번호는 카드 연결에만 사용되며 저장되지 않아요.</div>
          {error && <div className="spending-form-error"><DashboardIcon name="info" size={16} />{error}</div>}
          <button className="spending-setup-action" type="submit" disabled={isSubmitting || isIssuerLoading}>{isSubmitting ? '카드사에 연결하고 있어요' : showBirthDate ? '다시 연결하기' : '카드 연결하기'}</button>
          <button className="spending-setup-skip" type="button" onClick={onComplete}>우선 직접 입력하기</button>
        </form>
      </>}
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

function coachingCardContent(card) {
  const label = `${card.targetLabel || ''} ${card.title || ''}`
  const visual = /카페|간식/.test(label)
    ? { icon: 'coffee', tone: 'green' }
    : /배달|식비|외식/.test(label)
      ? { icon: 'utensils', tone: 'orange' }
      : /교통|택시|주유|차량/.test(label)
        ? { icon: 'car', tone: 'blue' }
        : /편의점|마트|쇼핑/.test(label)
          ? { icon: 'bag', tone: 'orange' }
          : { icon: 'sparkle', tone: 'purple' }
  const sentences = String(card.body || '').match(/[^.!?]+[.!?]?/g)?.map((sentence) => sentence.trim()).filter(Boolean) || []
  const savingIndex = sentences.findIndex((sentence) => /아낄|절감/.test(sentence))

  if (savingIndex >= 0) {
    return { ...visual, body: sentences.filter((_, index) => index !== savingIndex).join(' '), saving: sentences[savingIndex] }
  }
  return { ...visual, body: card.body, saving: toNumber(card.estimatedSaving) > 0 ? `예상 절감액 ${formatWon(card.estimatedSaving)}` : '' }
}

function CoachingCards({ data, error, onDismiss, pendingId, expanded }) {
  if (error) return <SectionError message={error} />
  if (!data) return <LoadingState />
  if (!data.items?.length) return <EmptyState icon="sparkle" title="아직 제공할 소비 코칭이 없어요" description={data.emptyReason === 'CARD_NOT_CONNECTED' ? '카드를 연결하면 소비 습관을 분석해드려요.' : '분석 가능한 소비내역이 쌓이면 맞춤 코칭을 보여드려요.'} />
  return <div id="spending-coaching-list" className={`spending-coaching-list${expanded ? ' expanded' : ''}`}>{data.items.map((card, index) => {
    const content = coachingCardContent(card)
    return <article className={index === 0 ? 'active' : ''} key={card.id}><span className={`spending-coach-icon ${content.tone}`}><DashboardIcon name={content.icon} size={18} /></span><h3>{card.title}</h3>{content.body && <p>{content.body}</p>}{content.saving && <strong>{content.saving}</strong>}<div className="spending-coach-actions"><button type="button" disabled={pendingId === card.id} onClick={() => onDismiss(card.id, 'NOT_WASTE')}>필요한 소비</button><button type="button" disabled={pendingId === card.id} onClick={() => onDismiss(card.id, 'HIDE')}>그만 보기</button></div></article>
  })}</div>
}

function CoachingPanel({ data, error, onDismiss, pendingId }) {
  const [expanded, setExpanded] = useState(false)
  const itemCount = data?.items?.length || 0
  const isExpanded = expanded && itemCount > 1

  return (
    <section className="spending-panel spending-coaching-panel">
      <div className="spending-panel-head">
        <PanelTitle icon="sparkle" title="이번 달 이렇게 아껴봐요" description="지난 소비 습관을 바탕으로 알려드려요" tone="purple" />
        {itemCount > 1 && <button type="button" className="spending-coach-expand" aria-expanded={isExpanded} aria-controls="spending-coaching-list" onClick={() => setExpanded((current) => !current)}>{isExpanded ? '접기' : `${itemCount}개 펼치기`}<span aria-hidden="true" /></button>}
      </div>
      <CoachingCards data={data} error={error} onDismiss={onDismiss} pendingId={pendingId} expanded={isExpanded} />
    </section>
  )
}

function SpendingGuidePage() {
  const [data, setData] = useState(EMPTY_DATA)
  const [sectionErrors, setSectionErrors] = useState({})
  const [pageError, setPageError] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [isEntryOpen, setIsEntryOpen] = useState(false)
  const [isBudgetOpen, setIsBudgetOpen] = useState(false)
  const [detailId, setDetailId] = useState(null)
  const [editingTransaction, setEditingTransaction] = useState(null)
  const [pendingCoachId, setPendingCoachId] = useState(null)
  const [initialSyncState, setInitialSyncState] = useState(null)
  const [retryingConnectionId, setRetryingConnectionId] = useState(null)

  const loadGuide = useCallback(async () => {
    setIsLoading(true)
    setPageError('')
    try {
      const summary = await getSpendingGuideSummary()
      if (summary.setupRequired) {
        setData({ ...EMPTY_DATA, summary })
        setSectionErrors({})
        setInitialSyncState(null)
        return
      }
      const syncState = await getInitialSyncStatus()
      if (INITIAL_SYNC_IN_PROGRESS.has(syncState?.overallStatus) || syncState?.overallStatus === 'FAILED') {
        setData({ ...EMPTY_DATA, summary })
        setSectionErrors({})
        setInitialSyncState(syncState)
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
      setInitialSyncState(syncState)
    } catch (error) {
      setPageError(errorMessage(error, '소비가이드를 불러오지 못했습니다.'))
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => { loadGuide() }, [loadGuide])

  useEffect(() => {
    if (!INITIAL_SYNC_IN_PROGRESS.has(initialSyncState?.overallStatus)) return undefined
    const intervalId = window.setInterval(async () => {
      try {
        const nextState = await getInitialSyncStatus()
        if (nextState?.overallStatus === 'COMPLETED' || nextState?.overallStatus == null) {
          window.clearInterval(intervalId)
          await loadGuide()
          return
        }
        setInitialSyncState(nextState)
      } catch {
        // 초기 수집은 백엔드에서 계속 진행되므로 다음 폴링에서 다시 확인한다.
      }
    }, 2000)
    return () => window.clearInterval(intervalId)
  }, [initialSyncState?.overallStatus, loadGuide])

  const handleCardConnected = useCallback(async (connection) => {
    setInitialSyncState({
      overallStatus: connection?.initialSyncStatus || 'NOT_STARTED',
      connections: connection ? [connection] : [],
    })
    await loadGuide()
  }, [loadGuide])

  const handleInitialSyncRetry = async (connectionId) => {
    setRetryingConnectionId(connectionId)
    try {
      await retryInitialSync(connectionId)
      setInitialSyncState((current) => ({ ...current, overallStatus: 'FETCHING' }))
    } catch (error) {
      setPageError(errorMessage(error, '카드 소비내역을 다시 수집하지 못했습니다.'))
    } finally {
      setRetryingConnectionId(null)
    }
  }

  const expandToday = async () => {
    try {
      const today = await getTodayTransactions(8)
      setData((current) => ({ ...current, today }))
    } catch (error) {
      setSectionErrors((current) => ({ ...current, today: errorMessage(error, '오늘 거래를 더 불러오지 못했습니다.') }))
    }
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
  const showInitialSync = INITIAL_SYNC_IN_PROGRESS.has(initialSyncState?.overallStatus) || initialSyncState?.overallStatus === 'FAILED'
  const showSetup = summary?.setupRequired && !showInitialSync

  return (
    <div className="dashboard spending-guide">
      <DashboardTopNav />
      <main className="spending-main">
        <header className="spending-page-head"><div><h1>{showSetup ? '소비가이드 설정' : '소비가이드'}</h1><p>{showSetup ? '처음 한 번만 입력하면 매일 소비 기준을 계산해드려요.' : '오늘의 기준을 확인하고, 무리 없이 쓸 수 있는 금액을 관리해보세요.'}</p></div></header>

        {pageError && <div className="spending-page-error"><DashboardIcon name="info" size={19} /><span>{pageError}</span><button type="button" onClick={loadGuide}>다시 시도</button></div>}
        {isLoading && !summary ? <LoadingState label="소비가이드를 불러오고 있어요." /> : showInitialSync ? <InitialSyncView summary={summary} syncState={initialSyncState} retryingId={retryingConnectionId} onRetry={handleInitialSyncRetry} /> : showSetup ? <SetupView onComplete={loadGuide} onCardConnected={handleCardConnected} /> : summary && <>
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
              <CoachingPanel data={data.coaching} error={sectionErrors.coaching} onDismiss={handleDismiss} pendingId={pendingCoachId} />
            </div>
          </div>
        </>}
      </main>
      <DashboardFooter />
      {isEntryOpen && <ManualTransactionModal categories={data.categories} allowCard={Boolean(data.connections && (!data.connections.connections?.length || !data.connections.cardSyncEnabled))} onClose={() => setIsEntryOpen(false)} onSaved={loadGuide} />}
      {isBudgetOpen && summary && <BudgetSettingsModal summary={summary} onClose={() => setIsBudgetOpen(false)} onSaved={loadGuide} />}
      {detailId && <TransactionDetailModal transactionId={detailId} categories={data.categories} onClose={() => setDetailId(null)} onChanged={loadGuide} onEdit={(transaction) => { setDetailId(null); setEditingTransaction(transaction) }} />}
      {editingTransaction && <ManualTransactionModal transaction={editingTransaction} categories={data.categories} allowCard={Boolean(data.connections && (!data.connections.connections?.length || !data.connections.cardSyncEnabled))} onClose={() => setEditingTransaction(null)} onSaved={loadGuide} />}
    </div>
  )
}

export default SpendingGuidePage
