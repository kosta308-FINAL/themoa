import { useState } from "react";
import DashboardIcon from "../../../components/common/DashboardIcon";

function AccountSecurityCard({
  onOpenChangePassword,
  onLogoutAllDevices,
  onOpenWithdraw,
}) {
  const [isLoggingOutAll, setIsLoggingOutAll] = useState(false);

  const handleLogoutAll = async () => {
    if (!window.confirm("이 계정으로 로그인된 모든 기기에서 로그아웃할까요?")) {
      return;
    }
    setIsLoggingOutAll(true);
    try {
      await onLogoutAllDevices();
    } finally {
      setIsLoggingOutAll(false);
    }
  };

  return (
    <section className="mp-card">
      <div className="mp-card-head">
        <span className="mp-card-icon">
          <DashboardIcon name="settings" size={17} />
        </span>
        <h2>계정 관리</h2>
      </div>
      <div className="mp-account-actions">
        <button
          type="button"
          className="mp-ghost-button"
          onClick={onOpenChangePassword}
        >
          비밀번호 변경
        </button>
        <button
          type="button"
          className="mp-danger-button"
          onClick={handleLogoutAll}
          disabled={isLoggingOutAll}
        >
          {isLoggingOutAll ? "로그아웃 중..." : "전체 기기 로그아웃"}
        </button>
      </div>
      <div className="mp-danger-zone">
        <div>
          <strong>회원 탈퇴</strong>
          <p>탈퇴하면 계정과 데이터가 지체 없이 파기되며 되돌릴 수 없어요.</p>
        </div>
        <button
          type="button"
          className="mp-text-danger-button"
          onClick={onOpenWithdraw}
        >
          탈퇴하기
        </button>
      </div>
    </section>
  );
}

export default AccountSecurityCard;
