import { Link } from "react-router-dom";
import BrandLogo from "../../components/common/BrandLogo";
import { useLandingScrollReveal } from "./hooks/useLandingScrollReveal";
import "./LandingPage.css";

const LOGIN_PATH = "/login";
const SIGNUP_PATH = "/signup";
const DASHBOARD_PATH = "/dashboard";

const problemCards = [
  "이번 달에 얼마가 남았는지 알기 어려워요",
  "카드값과 고정지출이 언제 나가는지 자꾸 놓쳐요",
  "나에게 맞는 정책과 금융상품을 찾기 어려워요",
];

const spendingMetrics = [
  { label: "오늘 사용 가능 금액", value: "110,000원" },
  { label: "하루 권장 소비액", value: "98,000원" },
  { label: "이번 주기 남은 예산", value: "1,540,000원" },
  { label: "월 저축 목표", value: "600,000원" },
];

const steps = [
  { number: "01", title: "월급과 저축 목표 설정" },
  { number: "02", title: "카드를 연결하거나 소비 직접 입력" },
  { number: "03", title: "오늘 사용할 수 있는 금액 확인" },
  { number: "04", title: "정책과 금융상품을 찾아 관심 목록으로 관리" },
];

const trustCards = [
  {
    title: "실제 소비 흐름 기반",
    desc: "월급, 고정지출, 저축 목표와 현재 소비 내역을 기준으로 현황을 확인합니다.",
  },
  {
    title: "내 조건에 맞는 탐색",
    desc: "정책과 금융상품을 자연어로 검색하고 관심 항목을 저장할 수 있습니다.",
  },
  {
    title: "흩어진 금융 정보 통합",
    desc: "소비, 고정지출, 정책, 금융상품 정보를 하나의 서비스에서 확인할 수 있습니다.",
  },
];

function LandingPage() {
  const landingPageRef = useLandingScrollReveal();
  const isLoggedIn = Boolean(localStorage.getItem("accessToken"));
  const startPath = isLoggedIn ? DASHBOARD_PATH : SIGNUP_PATH;
  const startLabel = isLoggedIn ? "내 대시보드로 이동" : "더모아 시작하기";

  const scrollToSection = (event, sectionId) => {
    event.preventDefault();
    document.getElementById(sectionId)?.scrollIntoView({ behavior: "smooth" });
  };

  return (
    <div className="landing-page" ref={landingPageRef}>
      <LandingHeader
        startPath={startPath}
        onSectionClick={scrollToSection}
      />
      <main className="landing-main">
        <HeroSection startPath={startPath} onSectionClick={scrollToSection} />
        <ProblemsSection />
        <FeaturesSection />
        <HowItWorksSection />
        <DashboardPreviewSection />
        <TrustSection />
        <FinalCtaSection startPath={startPath} startLabel={startLabel} />
      </main>
      <LandingFooter
        startPath={startPath}
        onSectionClick={scrollToSection}
      />
    </div>
  );
}

function LandingHeader({ startPath, onSectionClick }) {
  return (
    <header className="landing-header">
      <div className="landing-shell landing-header-inner">
        <BrandLogo
          className="landing-brand"
          to="/"
          label="themoa"
          size="medium"
          ariaLabel="themoa 홈으로 이동"
          onClick={(event) => onSectionClick(event, "landing-hero")}
        />
        <nav className="landing-nav" aria-label="랜딩 페이지 섹션">
          <a
            href="#landing-problems"
            className="landing-nav-link"
            onClick={(event) => onSectionClick(event, "landing-problems")}
          >
            서비스 소개
          </a>
          <a
            href="#landing-features"
            className="landing-nav-link"
            onClick={(event) => onSectionClick(event, "landing-features")}
          >
            주요 기능
          </a>
          <a
            href="#landing-how-it-works"
            className="landing-nav-link"
            onClick={(event) => onSectionClick(event, "landing-how-it-works")}
          >
            이용 방법
          </a>
        </nav>
        <div className="landing-header-actions">
          <Link to={LOGIN_PATH} className="landing-link-button">
            로그인
          </Link>
          <Link to={startPath} className="landing-primary-button">
            시작하기
          </Link>
        </div>
      </div>
    </header>
  );
}

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

function ProblemsSection() {
  return (
    <section className="landing-section landing-problems" id="landing-problems">
      <div className="landing-shell">
        <SectionIntro
          eyebrow="서비스 소개"
          title="돈을 기록하는 것만으로는 부족하니까"
          desc={
            <>
              더모아는 이미 쓴 돈만 보여주는 대신,
              <br className="landing-text-break" />
              앞으로 쓸 수 있는 금액과 놓치기 쉬운 금융 정보를 함께
              보여줍니다.
            </>
          }
        />
        <div className="landing-problem-grid">
          {problemCards.map((text, index) => (
            <article
              className="landing-problem-card"
              key={text}
              data-landing-reveal="up"
              style={{ "--landing-reveal-index": index }}
            >
              <SimpleIcon />
              <h3 className="landing-problem-title">{text}</h3>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

function FeaturesSection() {
  return (
    <section className="landing-section landing-features" id="landing-features">
      <div className="landing-shell">
        <SectionIntro
          eyebrow="주요 기능"
          title="복잡한 금융 흐름을 한곳에서"
          desc="소비 계획부터 관심 정책과 금융상품까지, 더모아의 주요 기능을 확인해보세요."
        />
        <FeatureBlock
          title="오늘 써도 되는 금액을 매일 다시 계산해요"
          desc={[
            "월급, 고정지출, 저축 목표와 남은 날짜를 기준으로 오늘 사용할 수 있는 금액을 계산합니다.",
            "예산보다 적게 쓰거나 많이 사용한 금액도 남은 기간에 맞춰 다시 반영합니다.",
          ]}
          preview={<SpendingGuidePreview />}
        />
        <FeatureBlock
          reverse
          title="반복되는 지출은 놓치지 않도록"
          desc={[
            "카드 거래에서 반복되는 결제를 확인하고 고정지출과 결제 예정 금액을 관리할 수 있습니다.",
            "소비 내역은 카테고리별로 나누어 어디에 많이 사용했는지 확인할 수 있습니다.",
          ]}
          preview={<ExpensePreview />}
        />
        <FeatureBlock
          title="내 조건에 맞는 혜택을 쉽게 찾아보세요"
          desc={[
            "자연어로 내 상황을 입력하면 조건과 관련된 지원 정책과 금융상품을 찾아볼 수 있습니다.",
            "관심 있는 정책과 상품은 저장해 마이페이지에서 다시 확인할 수 있습니다.",
          ]}
          preview={<SearchPreview />}
        />
        <FeatureBlock
          reverse
          title="찾아본 정책과 상품을 다시 놓치지 않도록"
          desc={[
            "관심 정책과 금융상품을 저장하고 마이페이지에서 한 번에 다시 확인할 수 있습니다.",
            "신청 기간과 상품 정보를 확인하며 나에게 필요한 항목을 정리해보세요.",
          ]}
          preview={<BookmarkPreview />}
        />
      </div>
    </section>
  );
}

function FeatureBlock({ title, desc, preview, reverse = false }) {
  const copyDirection = reverse ? "right" : "left";
  const previewDirection = reverse ? "left" : "right";

  return (
    <article
      className={
        reverse
          ? "landing-feature-block landing-feature-block-reverse"
          : "landing-feature-block"
      }
    >
      <div className="landing-feature-copy" data-landing-reveal={copyDirection}>
        <h3 className="landing-feature-title">{title}</h3>
        {desc.map((paragraph) => (
          <p className="landing-feature-desc" key={paragraph}>
            {paragraph}
          </p>
        ))}
      </div>
      <div
        className="landing-feature-preview"
        data-landing-reveal={previewDirection}
        style={{ "--landing-reveal-index": 1 }}
      >
        {preview}
      </div>
    </article>
  );
}

function SpendingGuidePreview() {
  return (
    <div className="landing-ui-card">
      <div className="landing-preview-label" data-landing-reveal="inner-up">서비스 화면 예시</div>
      <div className="landing-metric-grid">
        {spendingMetrics.map((metric, index) => (
          <PreviewMetric key={metric.label} {...metric} reveal="inner-up" revealIndex={index + 1} />
        ))}
      </div>
    </div>
  );
}

function ExpensePreview() {
  return (
    <div className="landing-ui-card">
      <div className="landing-preview-label" data-landing-reveal="inner-up">서비스 화면 예시</div>
      <div className="landing-split-preview">
        <div
          className="landing-list-panel"
          data-landing-reveal="inner-up"
          style={{ "--landing-reveal-index": 1 }}
        >
          <p className="landing-panel-title">고정지출 목록</p>
          <PreviewRow label="통신비 · 매월 12일" value="64,000원" reveal="inner-up" revealIndex={2} />
          <PreviewRow label="정기 구독 · 매월 18일" value="29,000원" reveal="inner-up" revealIndex={3} />
        </div>
        <div
          className="landing-list-panel"
          data-landing-reveal="inner-up"
          style={{ "--landing-reveal-index": 2 }}
        >
          <p className="landing-panel-title">카테고리 소비 비중</p>
          <CssBars />
          <PreviewRow label="최근 거래" value="카페 -6,500원" reveal="inner-up" revealIndex={4} />
        </div>
      </div>
    </div>
  );
}

function SearchPreview() {
  return (
    <div className="landing-ui-card">
      <div className="landing-preview-label" data-landing-reveal="inner-up">서비스 화면 예시</div>
      <div
        className="landing-search-box"
        data-landing-reveal="inner-up"
        style={{ "--landing-reveal-index": 1 }}
      >
        수원에 사는 27살 미취업 청년이 받을 수 있는 정책
      </div>
      <div
        className="landing-result-card landing-result-card-policy"
        data-landing-reveal="inner-up"
        style={{ "--landing-reveal-index": 2 }}
      >
        <span className="landing-result-kicker">입력 조건과 관련된 정책</span>
        <strong>청년 지원 정책</strong>
        <small>추가 자격 확인 필요</small>
      </div>
      <div
        className="landing-result-card"
        data-landing-reveal="inner-up"
        style={{ "--landing-reveal-index": 3 }}
      >
        <span className="landing-result-kicker">조건과 일치하는 항목</span>
        <strong>청년 우대 금융상품</strong>
        <small>관심 등록 가능</small>
      </div>
    </div>
  );
}

function BookmarkPreview() {
  return (
    <div className="landing-ui-card">
      <div className="landing-preview-label" data-landing-reveal="inner-up">서비스 화면 예시</div>
      <div
        className="landing-bookmark-tabs"
        data-landing-reveal="inner-up"
        style={{ "--landing-reveal-index": 1 }}
      >
        <span className="landing-bookmark-tab-active">마이페이지 북마크</span>
        <span>관심 정책 목록</span>
        <span>관심 상품 목록</span>
      </div>
      <PreviewRow label="청년 월세 지원" value="정책" reveal="inner-up" revealIndex={2} />
      <PreviewRow label="청년 우대 적금" value="금융상품" reveal="inner-up" revealIndex={3} />
    </div>
  );
}

function HowItWorksSection() {
  return (
    <section
      className="landing-section landing-how-it-works"
      id="landing-how-it-works"
    >
      <div className="landing-shell">
        <SectionIntro
          eyebrow="이용 방법"
          title="처음 시작해도 어렵지 않아요"
          desc="월급과 목표를 기준으로 소비 흐름을 잡고, 필요한 정책과 상품을 관심 목록으로 관리합니다."
        />
        <ol className="landing-step-list" data-landing-reveal="line">
          {steps.map((step, index) => (
            <li
              className="landing-step-item"
              key={step.number}
              data-landing-reveal="up"
              style={{ "--landing-reveal-index": index + 1 }}
            >
              <span className="landing-step-number">{step.number}</span>
              <h3 className="landing-step-title">{step.title}</h3>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}

function DashboardPreviewSection() {
  return (
    <section className="landing-section landing-preview" id="landing-preview">
      <div className="landing-shell">
        <SectionIntro
          eyebrow="대시보드 미리보기"
          title="내 금융 현황을 한눈에"
          desc={
            <>
              오늘의 소비 가능 금액부터 최근 소비,
              <br className="landing-text-break" />
              관심 정책과 금융상품까지 하나의 대시보드에서 확인할 수
              있습니다.
            </>
          }
        />
        <div className="landing-browser-frame" data-landing-reveal="scale">
          <div
            className="landing-browser-top"
            aria-hidden="true"
            data-landing-reveal="inner-up"
          >
            <span />
            <span />
            <span />
          </div>
          <div className="landing-browser-content">
            <div
              className="landing-preview-label"
              data-landing-reveal="inner-up"
              style={{ "--landing-reveal-index": 1 }}
            >
              서비스 화면 예시
            </div>
            <div className="landing-dashboard-grid">
              <PreviewMetric label="오늘 사용 가능 금액" value="110,000원" reveal="inner-up" revealIndex={2} />
              <PreviewMetric label="이번 주기 남은 예산" value="1,540,000원" reveal="inner-up" revealIndex={3} />
              <div
                className="landing-dashboard-card landing-dashboard-card-wide"
                data-landing-reveal="inner-up"
                style={{ "--landing-reveal-index": 4 }}
              >
                <p className="landing-panel-title">소비 분석</p>
                <CssBars />
              </div>
              <div
                className="landing-dashboard-card"
                data-landing-reveal="inner-up"
                style={{ "--landing-reveal-index": 5 }}
              >
                <p className="landing-panel-title">최근 소비</p>
                <PreviewRow label="카페" value="-6,500원" reveal="inner-up" revealIndex={6} />
                <PreviewRow label="정기 구독" value="-29,000원" reveal="inner-up" revealIndex={7} />
              </div>
              <div
                className="landing-dashboard-card"
                data-landing-reveal="inner-up"
                style={{ "--landing-reveal-index": 6 }}
              >
                <p className="landing-panel-title">관심 정책</p>
                <PreviewRow label="청년 지원 정책" value="3개" reveal="inner-up" revealIndex={7} />
              </div>
              <div
                className="landing-dashboard-card"
                data-landing-reveal="inner-up"
                style={{ "--landing-reveal-index": 7 }}
              >
                <p className="landing-panel-title">관심 금융상품</p>
                <PreviewRow label="청년 우대 적금" value="1개" reveal="inner-up" revealIndex={8} />
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function TrustSection() {
  return (
    <section className="landing-section landing-trust" id="landing-trust">
      <div className="landing-shell">
        <SectionIntro
          eyebrow="서비스 특징"
          title="내 상황을 기준으로 연결되는 금융 관리"
        />
        <div className="landing-trust-grid">
          {trustCards.map((card, index) => (
            <article
              className="landing-trust-card"
              key={card.title}
              data-landing-reveal="up"
              style={{ "--landing-reveal-index": index }}
            >
              <h3 className="landing-trust-title">{card.title}</h3>
              <p className="landing-trust-desc">{card.desc}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

function FinalCtaSection({ startPath, startLabel }) {
  return (
    <section className="landing-start" id="landing-start">
      <div className="landing-shell">
        <div className="landing-start-box" data-landing-reveal="scale">
          <h2
            className="landing-start-title"
            data-landing-reveal="inner-up"
            style={{ "--landing-reveal-index": 1 }}
          >
            다음 월급날까지,
            <br />
            더 계획적으로 관리해보세요.
          </h2>
          <p
            className="landing-start-desc"
            data-landing-reveal="inner-up"
            style={{ "--landing-reveal-index": 2 }}
          >
            복잡한 계산은 더모아에 맡기고
            <br className="landing-text-break" />
            오늘 사용할 수 있는 금액부터 확인해보세요.
          </p>
          <Link
            to={startPath}
            className="landing-start-button"
            data-landing-reveal="inner-up"
            style={{ "--landing-reveal-index": 3 }}
          >
            {startLabel}
          </Link>
        </div>
      </div>
    </section>
  );
}

function LandingFooter({ startPath, onSectionClick }) {
  return (
    <footer className="landing-footer">
      <div className="landing-shell landing-footer-inner">
        <div className="landing-footer-brand">
          <BrandLogo
            to="/"
            label=""
            size="medium"
            ariaLabel="themoa 홈으로 이동"
          />
          <div>
            <strong>themoa</strong>
            <p>월급 기반 금융 생활 관리 서비스</p>
          </div>
        </div>
        <nav className="landing-footer-links" aria-label="푸터 링크">
          <a
            href="#landing-problems"
            onClick={(event) => onSectionClick(event, "landing-problems")}
          >
            서비스 소개
          </a>
          <a
            href="#landing-features"
            onClick={(event) => onSectionClick(event, "landing-features")}
          >
            주요 기능
          </a>
          <a
            href="#landing-how-it-works"
            onClick={(event) => onSectionClick(event, "landing-how-it-works")}
          >
            이용 방법
          </a>
          <Link to={LOGIN_PATH}>로그인</Link>
          <Link to={startPath}>시작하기</Link>
        </nav>
        <p className="landing-footer-copy">
          Copyright © TheMoa. All rights reserved.
        </p>
      </div>
    </footer>
  );
}

function SectionIntro({ eyebrow, title, desc }) {
  return (
    <div className="landing-section-intro" data-landing-reveal="up">
      <p className="landing-section-eyebrow">{eyebrow}</p>
      <h2 className="landing-section-title">{title}</h2>
      {desc ? <p className="landing-section-desc">{desc}</p> : null}
    </div>
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

function CssBars() {
  return (
    <div className="landing-css-bars" aria-label="카테고리 소비 비중 예시">
      <span className="landing-css-bar landing-css-bar-food" style={{ "--landing-reveal-index": 0 }} />
      <span className="landing-css-bar landing-css-bar-life" style={{ "--landing-reveal-index": 1 }} />
      <span className="landing-css-bar landing-css-bar-save" style={{ "--landing-reveal-index": 2 }} />
    </div>
  );
}

function SimpleIcon() {
  return (
    <svg
      className="landing-simple-icon"
      width="36"
      height="36"
      viewBox="0 0 36 36"
      fill="none"
      aria-hidden="true"
    >
      <rect x="7" y="8" width="22" height="20" rx="6" stroke="currentColor" />
      <path d="M12 17h12M12 22h8" stroke="currentColor" strokeLinecap="round" />
      <circle cx="25" cy="11" r="4" fill="currentColor" opacity="0.16" />
    </svg>
  );
}

export default LandingPage;
