const dash = (value) => value || '-'

function PolicyResultCard({ item, active, onOpen }) {
  return (
    <button
      type="button"
      className={`policy-result-card${active ? ' active' : ''}`}
      onClick={() => onOpen(item.policyId)}
    >
      <div className="policy-result-title-row">
        <strong>{item.title}</strong>
        <span>{dash(item.applicationStatus)}</span>
      </div>
      <p>{dash(item.summary)}</p>
      <div className="policy-meta-row">
        <span>{dash(item.region)}</span>
        <span>{dash(item.agencyName)}</span>
        <span>{dash(item.category)}</span>
      </div>
      <div className="policy-meta-row">
        <span>연령 {dash(item.minAge)} - {dash(item.maxAge)}</span>
        <span>{dash(item.regionMatchLabel || item.regionCompatibility)}</span>
      </div>
    </button>
  )
}

export default PolicyResultCard
