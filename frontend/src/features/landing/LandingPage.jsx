import { Link } from 'react-router-dom'
import Header from '../../components/layout/Header'
import Footer from '../../components/layout/Footer'
import './LandingPage.css'

function LandingPage() {
  return (
    <div className="page-wrapper">
      <Header />
      <main>
        <HeroSection />
        <FeaturesSection />
      </main>
      <Footer />
    </div>
  )
}

/* ─── Slide 1: 하루 소비 가이드 ───────────────────────────── */
function Slide1() {
  return (
    <div className="slide-card">
      <div className="dp-header">
        <div className="dp-title">
          <span className="dp-greeting">안녕하세요, 솔민님</span>
          <span className="dp-sub">오늘 쓸 수 있는 금액</span>
        </div>
        <div className="dp-badge good">순항 중</div>
      </div>
      <div className="dp-amount-row">
        <span className="dp-amount">₩ 47,200</span>
      </div>
      <div className="dp-progress-bar">
        <div className="dp-progress-fill" style={{ width: '67%' }} />
      </div>
      <div className="dp-progress-note">이번달 목표의 67% 달성 중</div>
      <div className="dp-divider" />
      <div className="dp-section-label">지출 카테고리</div>
      <div className="dp-category-bar">
        <div className="cat-seg" style={{ width: '38%', background: '#f59e0b' }} />
        <div className="cat-seg" style={{ width: '25%', background: '#4f62e8' }} />
        <div className="cat-seg" style={{ width: '18%', background: '#22c55e' }} />
        <div className="cat-seg" style={{ width: '19%', background: '#e5e7eb' }} />
      </div>
      <div className="dp-cat-labels">
        <span><i className="cat-dot" style={{ background: '#f59e0b' }} />식비 38%</span>
        <span><i className="cat-dot" style={{ background: '#4f62e8' }} />쇼핑 25%</span>
        <span><i className="cat-dot" style={{ background: '#22c55e' }} />교통 18%</span>
        <span><i className="cat-dot" style={{ background: '#d1d5db' }} />기타 19%</span>
      </div>
      <div className="dp-divider" />
      <div className="dp-section-label">최근 거래</div>
      <div className="dp-txn-list">
        {[
          { name: '스타벅스', cat: '쇼핑 · 어제', amount: '-₩6,500', pos: false },
          { name: '쿠팡', cat: '쇼핑 · 어제', amount: '-₩24,900', pos: false },
          { name: '급여', cat: '수입 · 6/25', amount: '+₩2,800,000', pos: true },
        ].map((t, i) => (
          <div key={i} className="dp-txn">
            <div className="dp-txn-info">
              <span className="dp-txn-name">{t.name}</span>
              <span className="dp-txn-cat">{t.cat}</span>
            </div>
            <span className={`dp-txn-amount ${t.pos ? 'pos' : 'neg'}`}>{t.amount}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

/* ─── Hero Section ────────────────────────────────────────── */
function HeroSection() {
  return (
    <section className="hero" id="hero">
      <div className="hero-bg">
        <div className="hero-blob blob-1" />
        <div className="hero-blob blob-2" />
        <div className="hero-grid" />
      </div>
      <div className="container hero-split">

        {/* ── Left: Text ── */}
        <div className="hero-left">
          <div className="hero-badge">
            <span className="badge-dot" />
            내 월급, 내가 설계하는 소비
          </div>
          <h1 className="hero-title">
            내 월급, 어디로<br />
            사라지는지 이제<br />
            <span className="gradient-text">알 수 있어요.</span>
          </h1>
          <p className="hero-desc">
            Themore는 급여와 목표 기반으로 하루 소비 한도를 자동 계산하고,<br />
            목표 저축까지 역산해 알려주는 스마트 재정 관리 서비스예요.
          </p>
          <div className="hero-actions">
            <Link to="/dashboard" className="btn btn-primary btn-lg">시작하기</Link>
          </div>
          <ul className="hero-bullets">
            <li>
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none">
                <path d="M5 12l5 5L19 7" stroke="var(--accent)" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
              가입 후 3분이면 나의 소비 리포트 완성
            </li>
            <li>
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none">
                <path d="M5 12l5 5L19 7" stroke="var(--accent)" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
              월급 기반으로 하루 소비 한도를 자동 계산
            </li>
            <li>
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none">
                <path d="M5 12l5 5L19 7" stroke="var(--accent)" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
              목표 달성률 실시간 확인
            </li>
          </ul>
        </div>

        {/* ── Right: Slide1 (fixed card) ── */}
        <div className="hero-right">
          <Slide1 />
        </div>

      </div>
    </section>
  )
}

/* ─── Features ────────────────────────────────────────────── */
function FeaturesSection() {
  const features = [
    {
      id: 'spending',
      icon: (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
          <rect x="3" y="3" width="18" height="18" rx="3" stroke="currentColor" strokeWidth="1.8" />
          <path d="M7 12h10M7 8h10M7 16h6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
        </svg>
      ),
      color: 'accent',
      tag: '소비 분석',
      title: '하루 소비 가이드',
      desc: '월급과 저금 목표를 입력하면 하루 권장 소비량을 자동 계산해요. 요일별 패턴과 전월 비교까지 한눈에.',
      points: ['하루 권장 소비 역산', '요일별 소비 패턴 분석', '월 중반 자동 예산 재계산'],
    },
    {
      id: 'subscription',
      icon: (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
          <rect x="3" y="5" width="18" height="14" rx="2" stroke="currentColor" strokeWidth="1.8" />
          <path d="M3 9h18" stroke="currentColor" strokeWidth="1.8" />
          <circle cx="7" cy="13" r="1.5" fill="currentColor" />
          <path d="M11 13h6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
        </svg>
      ),
      color: 'violet',
      tag: '구독 관리',
      title: '고정지출 다이어트',
      desc: '구독 서비스를 한눈에 정리하고, 결제일 타임라인으로 이번 달 고정비를 파악하세요.',
      points: ['결제일 캘린더 타임라인', '순수 가용 자금 자동 계산', '연간 구독료 환산 표시'],
    },
    {
      id: 'policy',
      icon: (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
          <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" stroke="currentColor" strokeWidth="1.8" />
          <path d="M9 22V12h6v10" stroke="currentColor" strokeWidth="1.8" />
        </svg>
      ),
      color: 'green',
      tag: '정책 추천',
      title: '지역 혜택 한눈에',
      desc: '거주지, 나이, 소득 기준으로 받을 수 있는 정부·지자체 지원금을 자동으로 매칭해드려요.',
      points: ['거주지·나이 기반 자동 매칭', '마감 임박 정책 알림', '자연어로 정책 검색'],
    },
    {
      id: 'product',
      icon: (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
          <path d="M12 2L2 7l10 5 10-5-10-5z" stroke="currentColor" strokeWidth="1.8" />
          <path d="M2 17l10 5 10-5M2 12l10 5 10-5" stroke="currentColor" strokeWidth="1.8" />
        </svg>
      ),
      color: 'orange',
      tag: '금융상품',
      title: '맞춤 금융상품 추천',
      desc: '성향 진단 결과를 바탕으로 적금·예금·CMA를 추천하고, 목표 금액 도달까지 시뮬레이션해요.',
      points: ['성향 기반 Top 3 비교', '저금 목표 연동 시뮬레이션', '청년 특화 상품 우선 추천'],
    },
  ]

  return (
    <section className="features" id="features">
      <div className="container">
        <div className="section-header">
          <h2 className="section-title">내 돈을 위한 네 가지 도구</h2>
          <p className="section-desc">
            복잡한 가계부는 그만. 필요한 것만 똑똑하게 관리하세요.
          </p>
        </div>
        <div className="features-grid">
          {features.map((f) => (
            <div key={f.id} className={`feature-card feature-${f.color}`}>
              <div className="feature-card-top">
                <div className={`feature-icon-wrap icon-${f.color}`}>
                  {f.icon}
                </div>
                <span className={`feature-tag tag-${f.color}`}>{f.tag}</span>
              </div>
              <h3 className="feature-title">{f.title}</h3>
              <p className="feature-desc">{f.desc}</p>
              <ul className="feature-points">
                {f.points.map((p, i) => (
                  <li key={i} className="feature-point">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                      <path d="M5 12l5 5L19 7" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                    {p}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

export default LandingPage
