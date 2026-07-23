import { useState } from "react";
import { createPortal } from "react-dom";
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
  const [isMobileNavOpen, setIsMobileNavOpen] = useState(false);

  const handleLogout = async () => {
    await logout();
    navigate("/");
  };

  const closeMobileNav = () => setIsMobileNavOpen(false);

  return (
    <>
      <nav className="dash-topnav">
        <button
          type="button"
          className="dash-mobile-menu-btn"
          onClick={() => setIsMobileNavOpen(true)}
          aria-label="메뉴 열기"
        >
          <DashboardIcon name="menu" />
        </button>
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

      {isMobileNavOpen &&
        createPortal(
          <div
            className="dash-mobile-nav-backdrop"
            role="presentation"
            onMouseDown={closeMobileNav}
          >
            <div
              className="dash-mobile-nav-drawer"
              role="dialog"
              aria-modal="true"
              aria-label="메뉴"
              onMouseDown={(event) => event.stopPropagation()}
            >
              <div className="dash-mobile-nav-head">
                <BrandLogo
                  label="themoa"
                  size="small"
                  className="dash-topnav-logo"
                />
                <button
                  type="button"
                  className="dash-mobile-nav-close"
                  onClick={closeMobileNav}
                  aria-label="닫기"
                >
                  <DashboardIcon name="x" />
                </button>
              </div>
              <div className="dash-mobile-nav-items">
                {navItems.map((item, idx) => (
                  <NavLink
                    key={item.key}
                    to={idx === 0 ? "/dashboard" : `/dashboard/${item.key}`}
                    className={({ isActive }) =>
                      `dash-mobile-nav-item${isActive ? " active" : ""}`
                    }
                    end={idx === 0}
                    onClick={closeMobileNav}
                  >
                    <DashboardIcon name={item.icon} />
                    <span>{item.label}</span>
                  </NavLink>
                ))}
              </div>
              <button
                type="button"
                className="dash-mobile-nav-logout"
                onClick={() => {
                  closeMobileNav();
                  handleLogout();
                }}
              >
                로그아웃
              </button>
            </div>
          </div>,
          document.body,
        )}
    </>
  );
}

export default DashboardTopNav;
