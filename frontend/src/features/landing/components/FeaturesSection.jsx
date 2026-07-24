const spendingMetrics = [
  { label: "오늘 사용 가능 금액", value: "110,000원" },
  { label: "하루 권장 소비액", value: "98,000원" },
  { label: "이번 주기 남은 예산", value: "1,540,000원" },
  { label: "월 저축 목표", value: "600,000원" },
];

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

export default FeaturesSection;
