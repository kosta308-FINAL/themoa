import { Link } from "react-router-dom";

function DashboardFooter() {
  return (
    <footer className="dash-footer">
      <div className="dash-footer-top">
        <div className="dash-footer-logo">
          <span className="dash-logo-mark">M</span>
          <span className="dash-logo-text">더모아</span>
        </div>
        <nav className="dash-footer-links">
          <a href="#">이용약관</a>
          <a href="#">개인정보처리방침</a>
          <Link to="/dashboard/customer-service">고객센터</Link>
        </nav>
      </div>
      <p className="dash-footer-copyright">© 더모아. All rights reserved.</p>
    </footer>
  );
}

export default DashboardFooter;
