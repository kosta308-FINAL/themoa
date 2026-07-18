import { useCallback, useEffect, useState } from 'react'
import {
  correctTransactionAmount,
  correctTransactionCanceledAmount,
  deleteManualTransaction,
  getTransactionDetail,
  updateTransactionCategory,
  updateTransactionMemo,
} from '../../api/spendingGuideApi'
import DashboardIcon from '../../components/common/DashboardIcon'

const WON = new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 0 })
const toNumber = (value) => Number(value ?? 0)
const formatWon = (value) => `${WON.format(Math.abs(toNumber(value)))}원`
const signedAmount = (value) => {
  const amount = toNumber(value)
  return `${amount > 0 ? '-' : amount < 0 ? '+' : ''}${formatWon(amount)}`
}
const formatDateTime = (value) => value
  ? new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
  : '—'

function TransactionDetailModal({ transactionId, categories, onClose, onChanged, onEdit }) {
  const [transaction, setTransaction] = useState(null)
  const [categoryId, setCategoryId] = useState('')
  const [memo, setMemo] = useState('')
  const [canceledAmount, setCanceledAmount] = useState('')
  const [correctedAmount, setCorrectedAmount] = useState('')
  const [error, setError] = useState('')
  const [pending, setPending] = useState('')

  const load = useCallback(async () => {
    const response = await getTransactionDetail(transactionId)
    setTransaction(response)
    setCategoryId(String(response.categoryId || ''))
    setMemo(response.memo || '')
    setCanceledAmount(String(Number(response.canceledAmount || 0)))
    setCorrectedAmount(String(Number(response.amount || 0)))
  }, [transactionId])

  useEffect(() => {
    let active = true
    getTransactionDetail(transactionId)
      .then((response) => {
        if (!active) return
        setTransaction(response)
        setCategoryId(String(response.categoryId || ''))
        setMemo(response.memo || '')
        setCanceledAmount(String(Number(response.canceledAmount || 0)))
        setCorrectedAmount(String(Number(response.amount || 0)))
      })
      .catch((requestError) => { if (active) setError(requestError.response?.data?.message || '거래 상세를 불러오지 못했습니다.') })
    return () => { active = false }
  }, [transactionId])

  const save = async (type, request) => {
    setError('')
    setPending(type)
    try {
      await request()
      await load()
      await onChanged()
    } catch (requestError) {
      setError(requestError.response?.data?.message || '변경사항을 저장하지 못했습니다.')
    } finally {
      setPending('')
    }
  }

  const handleDelete = async () => {
    if (!window.confirm('이 직접 입력 거래를 삭제할까요?')) return
    setPending('delete')
    setError('')
    try {
      await deleteManualTransaction(transactionId)
      await onChanged()
      onClose()
    } catch (requestError) {
      setError(requestError.response?.data?.message || '거래를 삭제하지 못했습니다.')
    } finally {
      setPending('')
    }
  }

  return (
    <div className="spending-modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="spending-modal spending-detail-modal" role="dialog" aria-modal="true" aria-labelledby="transaction-detail-title" onMouseDown={(event) => event.stopPropagation()}>
        <div className="spending-modal-head"><div><h2 id="transaction-detail-title">거래 상세</h2><p>소비내역을 확인하고 필요한 항목만 수정해요.</p></div><button type="button" className="spending-modal-close" onClick={onClose} aria-label="닫기">×</button></div>
        {error && <div className="spending-section-error"><DashboardIcon name="info" size={18} />{error}</div>}
        {!error && !transaction && <div className="spending-loading"><span className="spending-spinner" />거래 상세를 불러오는 중...</div>}
        {transaction && <div className="spending-detail-body">
          <div className="spending-detail-hero"><span className="spending-transaction-icon"><DashboardIcon name={transaction.paymentMethod === 'CARD' ? 'card' : 'receipt'} size={20} /></span><strong>{signedAmount(transaction.netAmount)}</strong><p>{transaction.merchantDisplayName || transaction.merchantNameRaw}</p><small>{transaction.source === 'MANUAL' ? '직접 입력' : '카드 자동수집'}</small></div>
          <dl>
            <div><dt>사용 일시</dt><dd>{formatDateTime(transaction.usedAt)}</dd></div>
            <div><dt>결제수단</dt><dd>{transaction.paymentMethod === 'CASH' ? '현금' : transaction.paymentMethod === 'TRANSFER' ? '계좌이체' : [transaction.cardOrganizationName, transaction.cardNumberMasked].filter(Boolean).join(' · ') || '카드'}</dd></div>
            <div><dt>결제 금액</dt><dd>{formatWon(transaction.amount)}</dd></div>
            <div><dt>취소 반영액</dt><dd>{formatWon(transaction.canceledAmount)}</dd></div>
            {transaction.originalAmount != null && <div><dt>외화 원금</dt><dd>{transaction.originalAmount} {transaction.currencyCode}</dd></div>}
          </dl>
          <div className="spending-detail-editor"><label><span>카테고리</span><select value={categoryId} onChange={(event) => setCategoryId(event.target.value)}>{(categories || []).map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label><button type="button" disabled={pending || !categoryId || Number(categoryId) === transaction.categoryId} onClick={() => save('category', () => updateTransactionCategory(transactionId, Number(categoryId)))}>변경</button></div>
          <div className="spending-detail-editor"><label><span>메모</span><textarea value={memo} maxLength={2000} onChange={(event) => setMemo(event.target.value)} placeholder="메모 없음" /></label><button type="button" disabled={pending || memo === (transaction.memo || '')} onClick={() => save('memo', () => updateTransactionMemo(transactionId, memo.trim() || null))}>저장</button></div>
          {transaction.cancelAmountUncertain && <div className="spending-detail-editor"><label><span>실제 취소 금액</span><input inputMode="numeric" value={canceledAmount} onChange={(event) => setCanceledAmount(event.target.value.replace(/\D/g, ''))} /></label><button type="button" disabled={pending || canceledAmount === ''} onClick={() => save('cancel', () => correctTransactionCanceledAmount(transactionId, Number(canceledAmount)))}>반영</button></div>}
          {transaction.originalAmount != null && <div className="spending-detail-editor"><label><span>실제 원화 청구액</span><input inputMode="numeric" value={correctedAmount} onChange={(event) => setCorrectedAmount(event.target.value.replace(/\D/g, ''))} /></label><button type="button" disabled={pending || !Number(correctedAmount)} onClick={() => save('amount', () => correctTransactionAmount(transactionId, Number(correctedAmount)))}>반영</button></div>}
          {transaction.source === 'MANUAL' && <div className="spending-detail-actions"><button type="button" className="spending-secondary" disabled={pending} onClick={() => onEdit(transaction)}>거래 수정</button><button type="button" className="spending-danger" disabled={pending} onClick={handleDelete}>{pending === 'delete' ? '삭제 중...' : '삭제'}</button></div>}
        </div>}
      </section>
    </div>
  )
}

export default TransactionDetailModal
