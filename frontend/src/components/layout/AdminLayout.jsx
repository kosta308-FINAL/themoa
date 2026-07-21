import { useEffect, useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";
import { getAdminInquiries } from "../../api/customerServiceApi";
import "./AdminLayout.css";

const NAV_ITEMS = [
  {
    key: "customer-service",
    label: "1:1 문의 / 고객센터",
    to: "/admin/customer-service",
    icon: "M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z",
  },
  {
    key: "customer-ai-quality",
    label: "FAQ AI 품질관리",
    to: "/admin/customer-service/ai-quality",
    icon: "M12 3a4 4 0 0 0-4 4v1H7a3 3 0 0 0-3 3v5a3 3 0 0 0 3 3h10a3 3 0 0 0 3-3v-5a3 3 0 0 0-3-3h-1V7a4 4 0 0 0-4-4zm-2 5V7a2 2 0 1 1 4 0v1",
  },
  {
    key: "merchant-master",
    label: "가맹점 & 서비스 마스터",
    to: "/admin/merchants",
    icon: "M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z",
  },
];

function AdminLayout({ title, subtitle, children }) {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [pendingInquiryCount, setPendingInquiryCount] = useState(0);

  useEffect(() => {
    getAdminInquiries({ status: "PENDING", size: 1 })
      .then((data) => setPendingInquiryCount(data?.totalElements || 0))
      .catch(() => {});
  }, []);

  const handleLogout = async () => {
    await logout();
    navigate("/");
  };

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <div className="admin-sidebar-header">
          <span className="admin-brand-mark">M</span>
          <div>
            <span className="admin-brand-title">더모아 관리자</span>
            <span className="admin-brand-sub">Themoa Admin</span>
          </div>
        </div>

        <nav className="admin-sidebar-nav">
          <div className="admin-nav-group-title">운영 &amp; 지원</div>
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.key}
              to={item.to}
              className={({ isActive }) =>
                `admin-nav-btn${isActive ? " active" : ""}`
              }
            >
              <svg className="admin-icon" viewBox="0 0 24 24">
                <path d={item.icon} />
              </svg>
              {item.label}
              {item.key === "customer-service" &&
                Boolean(pendingInquiryCount) && (
                  <span className="admin-nav-badge">
                    {pendingInquiryCount}
                  </span>
                )}
            </NavLink>
          ))}
        </nav>

        <div className="admin-sidebar-footer">
          <div className="admin-avatar">관</div>
          <div className="admin-info">
            <div className="admin-name">관리자 계정</div>
            <div className="admin-role">ADMIN 권한</div>
          </div>
        </div>
      </aside>

      <div className="admin-main">
        <header className="admin-topbar">
          <div className="admin-topbar-title">
            <span>{title}</span>
            {subtitle && <span className="admin-topbar-sub">{subtitle}</span>}
          </div>
          <button
            type="button"
            className="admin-btn admin-btn-secondary"
            onClick={handleLogout}
          >
            로그아웃
          </button>
        </header>
        <main className="admin-content">{children}</main>
      </div>
    </div>
  );
}

export default AdminLayout;
