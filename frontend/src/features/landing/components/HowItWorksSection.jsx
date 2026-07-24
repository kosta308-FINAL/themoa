const steps = [
  { number: "01", title: "월급과 저축 목표 설정" },
  { number: "02", title: "카드를 연결하거나 소비 직접 입력" },
  { number: "03", title: "오늘 사용할 수 있는 금액 확인" },
  { number: "04", title: "정책과 금융상품을 찾아 관심 목록으로 관리" },
];

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

function SectionIntro({ eyebrow, title, desc }) {
  return (
    <div className="landing-section-intro" data-landing-reveal="up">
      <p className="landing-section-eyebrow">{eyebrow}</p>
      <h2 className="landing-section-title">{title}</h2>
      {desc ? <p className="landing-section-desc">{desc}</p> : null}
    </div>
  );
}

export default HowItWorksSection;
