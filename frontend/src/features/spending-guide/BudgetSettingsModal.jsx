import { useState } from 'react'
import { updateSalary, updateSavingsGoal } from '../../api/spendingGuideApi'
import DashboardIcon from '../../components/common/DashboardIcon'

const WON = new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 0 })
const digits = (value) => value.replace(/\D/g, '').slice(0, 12)

function BudgetSettingsModal({ summary, onClose, onSaved }) {
  const [salary, setSalary] = useState(String(Number(summary.salaryAmount || 0)))
  const [savingsGoal, setSavingsGoal] = useState(String(Number(summary.savingsGoalAmount || 0)))
  const [applyFrom, setApplyFrom] = useState('CURRENT_CYCLE')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)
    try {
      await Promise.all([
        updateSalary({ amount: Number(salary), applyFrom }),
        updateSavingsGoal({ amount: Number(savingsGoal), applyFrom }),
      ])
      await onSaved()
      onClose()
    } catch (requestError) {
      setError(requestError.response?.data?.message || '예산 기준을 저장하지 못했습니다.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="spending-modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="spending-modal spending-budget-modal" role="dialog" aria-modal="true" aria-labelledby="budget-settings-title" onMouseDown={(event) => event.stopPropagation()}>
        <div className="spending-modal-head"><div><h2 id="budget-settings-title">예산 기준</h2><p>월급과 저축 목표의 적용 시점을 선택해 변경해요.</p></div><button type="button" className="spending-modal-close" onClick={onClose} aria-label="닫기">×</button></div>
        <form className="spending-entry-form" onSubmit={handleSubmit}>
          <label className="wide"><span>월 실수령액 *</span><div className="spending-input-suffix"><input inputMode="numeric" value={salary ? WON.format(Number(salary)) : ''} onChange={(event) => setSalary(digits(event.target.value))} required /><em>원</em></div></label>
          <label className="wide"><span>월 저축 목표</span><div className="spending-input-suffix"><input inputMode="numeric" value={savingsGoal ? WON.format(Number(savingsGoal)) : ''} onChange={(event) => setSavingsGoal(digits(event.target.value))} /><em>원</em></div></label>
          <label className="wide"><span>적용 시점 *</span><select value={applyFrom} onChange={(event) => setApplyFrom(event.target.value)}><option value="CURRENT_CYCLE">이번 급여 주기부터</option><option value="NEXT_CYCLE">다음 급여 주기부터</option></select></label>
          <div className="spending-form-notice wide"><DashboardIcon name="info" size={17} /><span>현재 주기에 적용하면 남은 예산과 하루 권장액이 즉시 다시 계산됩니다.</span></div>
          {error && <div className="spending-form-error wide"><DashboardIcon name="info" size={16} />{error}</div>}
          <button type="submit" className="spending-primary wide" disabled={isSubmitting}>{isSubmitting ? '저장 중...' : '예산 기준 저장'}</button>
        </form>
      </section>
    </div>
  )
}

export default BudgetSettingsModal
