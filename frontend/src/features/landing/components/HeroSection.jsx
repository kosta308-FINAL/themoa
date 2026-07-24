import { Link } from "react-router-dom";

function HeroSection({ startPath, onSectionClick }) {
  return (
    <section className="landing-hero" id="landing-hero">
      <div className="landing-hero-ambient" aria-hidden="true">
        <span className="landing-hero-blob landing-hero-blob-a" />
        <span className="landing-hero-blob landing-hero-blob-b" />
      </div>
      <div className="landing-shell landing-hero-layout">
        <div className="landing-hero-copy">
          <p className="landing-hero-badge landing-hero-sequence">내 월급부터 다음 월급날까지</p>
          <h1 className="landing-hero-title">
            <span className="landing-hero-title-line landing-hero-sequence">
              월급이 들어온 날부터
            </span>
            <span className="landing-hero-title-line landing-hero-sequence">
              <span className="landing-hero-highlight">다음 월급날까지,</span>
            </span>
            <span className="landing-hero-title-line landing-hero-sequence">계획 있게.</span>
          </h1>
          <p className="landing-hero-desc landing-hero-sequence">
            하루 소비 한도부터 고정지출, 정책, 금융상품까지
            <br className="landing-text-break" />
            내 상황에 맞는 금융 흐름을 더모아에서 확인하세요.
          </p>
          <div className="landing-hero-actions landing-hero-sequence">
            <Link to={startPath} className="landing-primary-button landing-hero-button">
              무료로 시작하기
            </Link>
            <a
              href="#landing-features"
              className="landing-secondary-button landing-hero-button"
              onClick={(event) => onSectionClick(event, "landing-features")}
            >
              서비스 둘러보기
            </a>
          </div>
          <p className="landing-hero-note landing-hero-sequence">
            월급과 저축 목표를 설정하면 오늘 사용할 수 있는 금액을 확인할 수
            있어요.
          </p>
        </div>
        <HeroMiniDashboard />
      </div>
    </section>
  );
}

function HeroMiniDashboard() {
  return (
    <aside className="landing-mini-dashboard" aria-label="서비스 화면 예시">
      <div className="landing-preview-label landing-mini-sequence">서비스 화면 예시</div>
      <div className="landing-mini-head landing-mini-sequence">
        <span className="landing-mini-title">오늘 사용 가능 금액</span>
        <strong className="landing-mini-amount">110,000원</strong>
      </div>
      <div className="landing-mini-progress landing-mini-sequence" aria-hidden="true">
        <span className="landing-mini-progress-fill" />
      </div>
      <div className="landing-mini-grid">
        <PreviewMetric label="이번 주기 남은 예산" value="1,540,000원" revealIndex={3} sequenceClassName="landing-mini-sequence" />
        <PreviewMetric label="관심 정책" value="3개" revealIndex={4} sequenceClassName="landing-mini-sequence" />
        <PreviewMetric label="관심 금융상품" value="1개" revealIndex={5} sequenceClassName="landing-mini-sequence" />
      </div>
      <div className="landing-mini-list">
        <p className="landing-mini-list-title landing-mini-sequence" style={{ "--landing-reveal-index": 6 }}>최근 소비 예시</p>
        <PreviewRow label="카페" value="-6,500원" revealIndex={7} sequenceClassName="landing-mini-sequence" />
        <PreviewRow label="정기 구독" value="-29,000원" revealIndex={8} sequenceClassName="landing-mini-sequence" />
      </div>
    </aside>
  );
}

function PreviewMetric({ label, value, reveal, revealIndex, sequenceClassName }) {
  return (
    <div
      className={
        sequenceClassName
          ? `landing-metric-card ${sequenceClassName}`
          : "landing-metric-card"
      }
      data-landing-reveal={reveal}
      style={
        revealIndex === undefined
          ? undefined
          : { "--landing-reveal-index": revealIndex }
      }
    >
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function PreviewRow({ label, value, reveal, revealIndex, sequenceClassName }) {
  return (
    <div
      className={
        sequenceClassName
          ? `landing-preview-row ${sequenceClassName}`
          : "landing-preview-row"
      }
      data-landing-reveal={reveal}
      style={
        revealIndex === undefined
          ? undefined
          : { "--landing-reveal-index": revealIndex }
      }
    >
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

export default HeroSection;
