const EMPLOYMENT_LABELS = {
  EMPLOYED: '재직 중',
  UNEMPLOYED: '미취업',
}

const SEARCH_MODE_LABELS = {
  KEYWORD: '키워드 검색',
  CONDITION: '조건 검색',
  HYBRID: '통합 검색',
}

function PolicySearchStatus({ result }) {
  const condition = result?.interpretedCondition
  if (!condition) return null

  const region = [condition.province, condition.city, condition.district].filter(Boolean).join(' ')
  const age = condition.age ?? condition.inferredAge
  const employmentLabel = EMPLOYMENT_LABELS[condition.employmentStatus]
  const searchModeLabel = SEARCH_MODE_LABELS[result.searchMode]
  const items = []

  if (region) {
    items.push(['지역', region])
  }

  if (age != null) {
    items.push(['나이', `${age}세`])
  }

  if (employmentLabel) {
    items.push(['취업', employmentLabel])
  }

  if (condition.studentStatus === true) {
    items.push(['학생', '해당'])
  }

  if (searchModeLabel) {
    items.push(['검색 모드', searchModeLabel])
  }

  if (items.length === 0) return null

  return (
    <section className="policy-condition-strip">
      {items.map(([label, value]) => (
        <span key={label}>{label}: {value}</span>
      ))}
    </section>
  )
}

export default PolicySearchStatus
