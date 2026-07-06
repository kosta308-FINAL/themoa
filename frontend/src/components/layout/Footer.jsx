function Footer() {
  return (
    <footer className="footer">
      <div className="container footer-inner">
        <div className="footer-logo">
          <div className="logo-mark">
            <svg width="18" height="18" viewBox="0 0 20 20" fill="none">
              <path d="M10 2L2 7v6l8 5 8-5V7L10 2z" fill="var(--accent)" opacity="0.15" />
              <path d="M10 2L2 7l8 5 8-5-8-5z" fill="var(--accent)" />
              <path d="M2 7v6l8 5V12L2 7z" fill="var(--accent)" opacity="0.7" />
              <path d="M18 7v6l-8 5V12l8-5z" fill="var(--accent)" opacity="0.5" />
            </svg>
          </div>
          <span className="logo-text" style={{ fontSize: '15px' }}>Themore</span>
        </div>
        <p className="footer-copy">© 2026 Themore. 내 소비의 기준을 만들다.</p>
        <div className="footer-links">
          <a href="#">이용약관</a>
          <a href="#">개인정보처리방침</a>
          <a href="#">문의하기</a>
        </div>
      </div>
    </footer>
  )
}

export default Footer
