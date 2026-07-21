import { Link } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";

function Header() {
  const { isAuthenticated } = useAuth();

  return (
    <header className="header">
      <div className="header-inner container">
        <div className="logo">
          <div className="logo-mark">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path
                d="M10 2L2 7v6l8 5 8-5V7L10 2z"
                fill="var(--accent)"
                opacity="0.15"
              />
              <path d="M10 2L2 7l8 5 8-5-8-5z" fill="var(--accent)" />
              <path d="M2 7v6l8 5V12L2 7z" fill="var(--accent)" opacity="0.7" />
              <path
                d="M18 7v6l-8 5V12l8-5z"
                fill="var(--accent)"
                opacity="0.5"
              />
            </svg>
          </div>
          <span className="logo-text">Themore</span>
        </div>
        <div className="header-actions">
          {isAuthenticated ? (
            <Link to="/dashboard" className="btn btn-ghost">
              대시보드로 이동
            </Link>
          ) : (
            <Link to="/login" className="btn btn-ghost">
              로그인
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}

export default Header;
