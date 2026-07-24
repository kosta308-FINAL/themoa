import { Link } from "react-router-dom";
import BrandLogo from "../../../components/common/BrandLogo";

function LandingHeader({
  isAuthenticated,
  loginPath,
  signupPath,
  dashboardPath,
  onSectionClick,
}) {
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
          {isAuthenticated ? (
            <Link to={dashboardPath} className="landing-primary-button">
              내 대시보드
            </Link>
          ) : (
            <>
              <Link to={loginPath} className="landing-link-button">
                로그인
              </Link>
              <Link to={signupPath} className="landing-primary-button">
                시작하기
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}

export default LandingHeader;
