import { useState } from 'react'
import EmptyState from './EmptyState'
import './DashboardPage.css'

const CATEGORIES = [
  { name: '식비', percent: 32, amount: '399,500원', color: '#0d9488' },
  { name: '교통', percent: 18, amount: '224,300원', color: '#22c55e' },
  { name: '쇼핑', percent: 15, amount: '187,200원', color: '#38bdf8' },
  { name: '문화/여가', percent: 12, amount: '149,800원', color: 'var(--pink)' },
  { name: '주거', percent: 10, amount: '124,000원', color: 'var(--orange)' },
  { name: '기타', percent: 13, amount: '163,700원', color: '#facc15' },
]

const POLICIES = [
  {
    name: '청년 월세 지원',
    desc: '월 20만원씩 12개월 지원',
    color: 'red',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
        <path d="M3 11l9-8 9 8" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M5 10v9a1 1 0 001 1h12a1 1 0 001-1v-9" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M9 20v-6h6v6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    ),
  },
  {
    name: '국가장학금',
    desc: '연 최대 700만원 지원',
    color: 'accent',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
        <path d="M2 8l10-5 10 5-10 5-10-5z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
        <path d="M6 10.5V16c0 1.1 2.7 3 6 3s6-1.9 6-3v-5.5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M22 8v6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
      </svg>
    ),
  },
  {
    name: '청년내일채움공제',
    desc: '2년 만기 시 최대 1,200만원',
    color: 'pink',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
        <path d="M12 20s-8-4.9-8-10.6C4 6 6.2 4 9 4c1.6 0 3 0.8 3 0.8S13.4 4 15 4c2.8 0 5 2 5 5.4C20 15.1 12 20 12 20z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
      </svg>
    ),
  },
]

function DonutChart({ data }) {
  const size = 150
  const r = 55
  const strokeWidth = 18
  const circumference = 2 * Math.PI * r
  let cumulative = 0

  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className="donut-svg">
      <g transform={`rotate(-90 ${size / 2} ${size / 2})`}>
        {data.map((d) => {
          const dash = (d.percent / 100) * circumference
          const offset = -((cumulative / 100) * circumference)
          cumulative += d.percent
          return (
            <circle
              key={d.name}
              cx={size / 2}
              cy={size / 2}
              r={r}
              fill="none"
              stroke={d.color}
              strokeWidth={strokeWidth}
              strokeDasharray={`${dash} ${circumference - dash}`}
              strokeDashoffset={offset}
            />
          )
        })}
      </g>
    </svg>
  )
}

function ChevronRight() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none">
      <path d="M9 6l6 6-6 6" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function ConsumptionCard() {
  const [showEmpty, setShowEmpty] = useState(false)

  return (
    <section className="dash-card">
      <div className="dash-card-header">
        <h3>소비 분석</h3>
        <button type="button" className="more-link" onClick={() => setShowEmpty((v) => !v)}>
          {showEmpty ? '다시 보기' : '더보기'} <ChevronRight />
        </button>
      </div>

      {showEmpty ? (
        <EmptyState
          compact
          title="데이터를 입력해주세요"
          description="소비 내역을 등록하면 카테고리별 자세한 분석을 볼 수 있어요"
        />
      ) : (
        <div className="consumption-body">
          <div className="donut-wrap">
            <DonutChart data={CATEGORIES} />
            <div className="donut-center">
              <span className="donut-total">1,248,500원</span>
              <span className="donut-sub">이번 달</span>
            </div>
          </div>
          <ul className="legend-list">
            {CATEGORIES.map((c) => (
              <li key={c.name}>
                <span className="legend-dot" style={{ background: c.color }} />
                <span className="legend-name">{c.name}</span>
                <span className="legend-percent">{c.percent}%</span>
                <span className="legend-amount">{c.amount}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  )
}

function ProductCard({ onMore }) {
  return (
    <section className="dash-card">
      <div className="dash-card-header">
        <h3>AI 추천 금융상품</h3>
        <button type="button" className="more-link" onClick={onMore}>
          더보기 <ChevronRight />
        </button>
      </div>

      <div className="product-highlight">
        <span className="product-label">추천 적금</span>
        <h4 className="product-title">우리 청년 우대 적금</h4>
        <div className="product-tags">
          <span className="product-tag">비대면</span>
          <span className="product-tag">우대금리</span>
          <span className="product-tag">높은 금리</span>
        </div>
        <div className="product-rate-row">
          <span className="product-rate-label">최고 연</span>
          <span className="product-rate">4.50%</span>
        </div>
        <button type="button" className="product-cta">자세히 보기</button>

        <svg className="product-illustration" width="88" height="88" viewBox="0 0 88 88" fill="none">
          <rect x="14" y="34" width="52" height="38" rx="4" fill="var(--green)" opacity="0.9" />
          <path d="M10 34l30-18 30 18" stroke="var(--green)" strokeWidth="4" strokeLinecap="round" strokeLinejoin="round" />
          <rect x="22" y="44" width="7" height="20" fill="#fff" opacity="0.35" />
          <rect x="37" y="44" width="7" height="20" fill="#fff" opacity="0.35" />
          <rect x="52" y="44" width="7" height="20" fill="#fff" opacity="0.35" />
          <circle cx="68" cy="24" r="12" fill="#facc15" stroke="#fff" strokeWidth="2" />
          <text x="68" y="29" textAnchor="middle" fontSize="12" fontWeight="700" fill="#a16207">M</text>
          <path d="M18 18c8-10 16-4 12 6-8 4-16-1-12-6z" fill="var(--green)" opacity="0.5" />
        </svg>
      </div>
    </section>
  )
}

function PolicyCard({ onMore }) {
  return (
    <section className="dash-card">
      <div className="dash-card-header">
        <h3>정부지원정책 추천</h3>
        <button type="button" className="more-link" onClick={onMore}>
          더보기 <ChevronRight />
        </button>
      </div>

      <ul className="policy-list">
        {POLICIES.map((p) => (
          <li key={p.name} className="policy-row">
            <div className={`policy-icon-wrap icon-${p.color}`}>{p.icon}</div>
            <div className="policy-info">
              <span className="policy-title">{p.name}</span>
              <span className="policy-desc">{p.desc}</span>
            </div>
            <span className="policy-badge">신청가능</span>
          </li>
        ))}
      </ul>
    </section>
  )
}

function DashboardPage({ onNavigate }) {
  return (
    <>
      <div className="dash-heading">
        <h2>대시보드</h2>
        <p>이번 달 재정 현황을 한눈에 확인하세요</p>
      </div>
      <div className="dash-grid">
        <ConsumptionCard />
        <ProductCard onMore={() => onNavigate('products')} />
        <PolicyCard onMore={() => onNavigate('policies')} />
      </div>
    </>
  )
}

export default DashboardPage
