import {
  NavLink,
  useNavigate,
} from "react-router-dom";
import DashboardIcon from "../common/DashboardIcon";
import NotificationBell from "./NotificationBell";
import { navItems } from "../../constants/dashboardNavigation";
import { useAuth } from "../../hooks/useAuth";
import BrandLogo from "../common/BrandLogo";

function DashboardTopNav() {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/");
  };

  return (
    <nav className="dash-topnav">
      <BrandLogo
        to="/dashboard"
        label="themoa"
        size="small"
        className="dash-topnav-logo"
        ariaLabel="themoa 홈 대시보드로 이동"
      />
      <div className="dash-topnav-right">
        <div className="dash-topnav-items">
          {navItems.map((item, idx) => (
            <NavLink
              key={item.key}
              to={idx === 0 ? "/dashboard" : `/dashboard/${item.key}`}
              className={({ isActive }) =>
                `dash-nav-item${isActive ? " active" : ""}`
              }
              end={idx === 0}
            >
              <DashboardIcon name={item.icon} />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </div>
        <NotificationBell />
        <button
          type="button"
          className="dash-logout-btn"
          onClick={handleLogout}
        >
          로그아웃
        </button>
      </div>
    </nav>
  );
}

export default DashboardTopNav;
