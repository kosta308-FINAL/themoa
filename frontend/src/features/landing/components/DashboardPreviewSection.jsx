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

export default DashboardPreviewSection;
