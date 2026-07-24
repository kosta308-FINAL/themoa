const problemCards = [
  "이번 달에 얼마가 남았는지 알기 어려워요",
  "카드값과 고정지출이 언제 나가는지 자꾸 놓쳐요",
  "나에게 맞는 정책과 금융상품을 찾기 어려워요",
];

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

function SectionIntro({ eyebrow, title, desc }) {
  return (
    <div className="landing-section-intro" data-landing-reveal="up">
      <p className="landing-section-eyebrow">{eyebrow}</p>
      <h2 className="landing-section-title">{title}</h2>
      {desc ? <p className="landing-section-desc">{desc}</p> : null}
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

export default ProblemsSection;
