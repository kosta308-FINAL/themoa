import { Link } from "react-router-dom";

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

export default FinalCtaSection;
