import { spendingTip } from '../../constants/mockDashboard'

function SpendingTipCard() {
  return (
    <div className="tip-card">
      <h3>{spendingTip.title}</h3>
      <p>{spendingTip.message}</p>
      <button type="button" className="tip-cta">{spendingTip.cta}</button>
    </div>
  )
}

export default SpendingTipCard
