import { Link } from "react-router-dom";
import BrandLogo from "../../../components/common/BrandLogo";

function LandingFooter({ loginPath, startPath, onSectionClick }) {
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
          <Link to={loginPath}>로그인</Link>
          <Link to={startPath}>시작하기</Link>
        </nav>
        <p className="landing-footer-copy">
          Copyright © TheMoa. All rights reserved.
        </p>
      </div>
    </footer>
  );
}

export default LandingFooter;
