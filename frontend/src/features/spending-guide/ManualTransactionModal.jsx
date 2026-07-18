import { useState } from 'react'
import { createManualTransaction, updateManualTransaction } from '../../api/spendingGuideApi'
import DashboardIcon from '../../components/common/DashboardIcon'

const WON = new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 0 })
const nowValue = () => {
  const now = new Date()
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset())
  return now.toISOString().slice(0, 16)
}

function ManualTransactionModal({ categories, transaction = null, allowCard = false, onClose, onSaved }) {
  const [form, setForm] = useState({
    amount: transaction ? String(Number(transaction.amount)) : '',
    paymentMethod: transaction?.paymentMethod || '',
    merchantName: transaction?.merchantDisplayName || transaction?.merchantNameRaw || '',
    usedAt: transaction?.usedAt?.slice(0, 16) || nowValue(),
    categoryId: transaction?.categoryId ? String(transaction.categoryId) : '',
    memo: transaction?.memo || '',
  })
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const update = (key) => (event) => setForm((current) => ({ ...current, [key]: event.target.value }))
  const handleAmount = (event) => setForm((current) => ({
    ...current,
    amount: event.target.value.replace(/\D/g, '').slice(0, 12),
  }))

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)
    const [usedDate, usedTime] = form.usedAt.split('T')
    const payload = {
      paymentMethod: form.paymentMethod,
      usedDate,
      usedTime: usedTime ? `${usedTime}:00` : null,
      amount: Number(form.amount),
      categoryId: Number(form.categoryId),
      merchantName: form.merchantName.trim(),
      memo: form.memo.trim() || null,
    }
    try {
      if (transaction) await updateManualTransaction(transaction.id, payload)
      else await createManualTransaction(payload)
      await onSaved()
      onClose()
    } catch (requestError) {
      setError(requestError.response?.data?.message || `지출을 ${transaction ? '수정' : '기록'}하지 못했습니다.`)
    } finally {
      setIsSubmitting(false)
    }
  }

  const showCard = allowCard || transaction?.paymentMethod === 'CARD'

  return (
    <div className="spending-modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="spending-modal" role="dialog" aria-modal="true" aria-labelledby="manual-entry-title" onMouseDown={(event) => event.stopPropagation()}>
        <div className="spending-modal-head"><div><h2 id="manual-entry-title">{transaction ? '지출 수정' : '지출 직접 입력'}</h2><p>직접 기록한 소비내역을 입력해요.</p></div><button type="button" className="spending-modal-close" onClick={onClose} aria-label="닫기">×</button></div>
        <form className="spending-entry-form" onSubmit={handleSubmit}>
          <label><span>금액 *</span><div className="spending-input-suffix"><input inputMode="numeric" value={form.amount ? WON.format(Number(form.amount)) : ''} onChange={handleAmount} placeholder="0" required /><em>원</em></div></label>
          <label><span>결제수단 *</span><select value={form.paymentMethod} onChange={update('paymentMethod')} required><option value="" disabled>선택</option>{showCard && <option value="CARD">카드</option>}<option value="CASH">현금</option><option value="TRANSFER">계좌이체</option></select></label>
          <label className="wide"><span>사용처/내용 *</span><input value={form.merchantName} onChange={update('merchantName')} maxLength={255} placeholder="사용처나 지출 내용을 입력하세요" required /></label>
          <label><span>사용일시 *</span><input type="datetime-local" value={form.usedAt} onChange={update('usedAt')} max={nowValue()} required /></label>
          <label><span>카테고리 *</span><select value={form.categoryId} onChange={update('categoryId')} required><option value="" disabled>선택</option>{(categories || []).map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label>
          <label className="wide"><span>메모</span><textarea value={form.memo} onChange={update('memo')} maxLength={2000} placeholder="기억해둘 내용을 적어주세요" /></label>
          <div className="spending-form-notice wide"><DashboardIcon name="info" size={17} /><span>{allowCard ? '카드 자동수집이 꺼져 있어 카드 지출도 직접 입력할 수 있어요.' : '카드 자동수집 중에는 현금과 계좌이체만 직접 입력할 수 있어요.'} 매달 반복되는 지출은 고정지출 메뉴에서 등록해주세요.</span></div>
          {error && <div className="spending-form-error wide"><DashboardIcon name="info" size={16} />{error}</div>}
          <button type="submit" className="spending-primary wide" disabled={isSubmitting || !categories?.length}>{isSubmitting ? '저장 중...' : transaction ? '지출 수정하기' : '지출 기록하기'}</button>
        </form>
      </section>
    </div>
  )
}

export default ManualTransactionModal
