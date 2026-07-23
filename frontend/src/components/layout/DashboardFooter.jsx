import { Link } from "react-router-dom";
import BrandLogo from "../common/BrandLogo";

function DashboardFooter() {
  return (
    <footer className="dash-footer">
      <div className="dash-footer-top">
        <BrandLogo
          to="/dashboard"
          label="themoa"
          size="small"
          className="dash-footer-logo"
          ariaLabel="themoa 홈 대시보드로 이동"
        />
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
