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

function SectionIntro({ eyebrow, title, desc }) {
  return (
    <div className="landing-section-intro" data-landing-reveal="up">
      <p className="landing-section-eyebrow">{eyebrow}</p>
      <h2 className="landing-section-title">{title}</h2>
      {desc ? <p className="landing-section-desc">{desc}</p> : null}
    </div>
  );
}

export default TrustSection;
