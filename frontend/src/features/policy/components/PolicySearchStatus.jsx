const dash = (value) => value || '-'

function PolicySearchStatus({ result }) {
  const condition = result?.interpretedCondition
  if (!condition) return null

  return (
    <section className="policy-condition-strip">
      <span>지역: {dash([condition.province, condition.city, condition.district].filter(Boolean).join(' '))}</span>
      <span>나이: {dash(condition.age || condition.inferredAge)}</span>
      <span>취업: {dash(condition.employmentStatus)}</span>
      <span>학생: {condition.studentStatus ? '해당' : '-'}</span>
      <span>검색 모드: {dash(result.searchMode)}</span>
    </section>
  )
}

export default PolicySearchStatus
