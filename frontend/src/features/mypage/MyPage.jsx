import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getApiErrorMessage } from "../../utils/apiError";
import { getMyPage } from "../../api/mypageApi";
import { getCardConnections } from "../../api/spendingGuideApi";
import { logoutAllDevices } from "../../api/authApi";
import { useAuth } from "../../hooks/useAuth";
import DashboardIcon from "../../components/common/DashboardIcon";
import ProfileCard from "./components/ProfileCard";
import SavingsGoalCard from "./components/SavingsGoalCard";
import CardConnectionCard from "./components/CardConnectionCard";
import AccountSecurityCard from "./components/AccountSecurityCard";
import ChangePasswordModal from "./components/ChangePasswordModal";
import TermsHistoryCard from "./components/TermsHistoryCard";
import ComingSoonCard from "./components/ComingSoonCard";
import PolicyBookmarksCard from "./components/PolicyBookmarksCard";
import {
  ENTRY_MODE_LABELS,
  INCOME_TYPE_LABELS,
  formatDate,
  formatWon,
} from "./mypageUtils";
import "./MyPage.css";

const TABS = [
  { key: "profile", label: "회원 정보", icon: "user" },
  { key: "cards", label: "카드 연동", icon: "card" },
  { key: "account", label: "계정 관리", icon: "settings" },
  { key: "soon", label: "북마크 · 진단", icon: "sparkle" },
];

function MyPage() {
  const navigate = useNavigate();
  const { logout } = useAuth();

  const [myPage, setMyPage] = useState(null);
  const [cardConnections, setCardConnections] = useState(null);
  const [pageError, setPageError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [toast, setToast] = useState("");
  const [activeTab, setActiveTab] = useState("profile");

  const showToast = (message) => setToast(message);

  useEffect(() => {
    if (!toast) return undefined;
    const timer = setTimeout(() => setToast(""), 2200);
    return () => clearTimeout(timer);
  }, [toast]);

  const load = useCallback(async () => {
    setIsLoading(true);
    setPageError("");
    const [myPageResult, connectionsResult] = await Promise.allSettled([
      getMyPage(),
      getCardConnections(),
    ]);
    if (myPageResult.status === "fulfilled") {
      setMyPage(myPageResult.value);
    } else {
      setPageError(
        getApiErrorMessage(
          myPageResult.reason,
          "마이페이지 정보를 불러오지 못했어요.",
        ),
      );
    }
    setCardConnections(
      connectionsResult.status === "fulfilled" ? connectionsResult.value : null,
    );
    setIsLoading(false);
  }, []);

  useEffect(() => {
    const run = () => load();
    run();
  }, [load]);

  const handleSavingsGoalSaved = async (message) => {
    await load();
    if (message) showToast(message);
  };

  const endSessionAndRedirect = async (path) => {
    await logout();
    navigate(path);
  };

  const handlePasswordChanged = async () => {
    setIsChangingPassword(false);
    await endSessionAndRedirect("/login");
  };

  const handleLogoutAllDevices = async () => {
    await logoutAllDevices();
    await endSessionAndRedirect("/");
  };

  const profile = myPage?.profile;
  const initial = profile?.name ? profile.name.slice(0, 1) : "회";

  return (
    <div className="mypage">
      <main className="mp-page">
        <div className="mp-page-head">
          <div>
            <h1>마이페이지</h1>
            <p>회원 정보, 저축목표, 카드 연동, 계정 관리를 한곳에서 확인해요.</p>
          </div>
        </div>

        {pageError && (
          <div className="mp-page-error">
            <DashboardIcon name="info" size={18} />
            <span>{pageError}</span>
            <button type="button" onClick={load}>
              다시 시도
            </button>
          </div>
        )}

        {isLoading && !myPage ? (
          <div className="mp-loading">마이페이지를 불러오고 있어요.</div>
        ) : (
          myPage && (
            <>
              <section className="mp-hero" aria-label="회원 요약">
                <div className="mp-hero-main">
                  <span className="mp-avatar" aria-hidden="true">
                    {initial}
                  </span>
                  <div>
                    <h2>{profile.name}님</h2>
                    <span>{profile.email}</span>
                    <div className="mp-hero-badges">
                      <span className="mp-badge">
                        {INCOME_TYPE_LABELS[profile.incomeType] ||
                          profile.incomeType}
                      </span>
                      <span className="mp-badge">
                        {ENTRY_MODE_LABELS[profile.entryMode] ||
                          profile.entryMode}
                      </span>
                      {profile.payday != null && (
                        <span className="mp-badge">
                          매월 {profile.payday}일 급여
                        </span>
                      )}
                    </div>
                  </div>
                </div>
                <div className="mp-hero-stats">
                  <div>
                    <span>월 저축목표</span>
                    <strong>{formatWon(myPage.savingsTargetAmount)}</strong>
                  </div>
                  <div>
                    <span>카드 자동수집</span>
                    <strong>
                      {profile.entryMode === "CARD" && profile.cardSyncEnabled
                        ? "ON"
                        : "OFF"}
                    </strong>
                  </div>
                  <div>
                    <span>가입일</span>
                    <strong>{formatDate(profile.createdAt)}</strong>
                  </div>
                </div>
              </section>

              <div className="mp-tabs" role="tablist" aria-label="마이페이지 섹션">
                {TABS.map((tab) => (
                  <button
                    key={tab.key}
                    type="button"
                    role="tab"
                    aria-selected={activeTab === tab.key}
                    className={`mp-tab${activeTab === tab.key ? " active" : ""}`}
                    onClick={() => setActiveTab(tab.key)}
                  >
                    <DashboardIcon name={tab.icon} size={15} />
                    {tab.label}
                  </button>
                ))}
              </div>

              {activeTab === "profile" && (
                <div className="mp-tab-panel mp-split">
                  <ProfileCard profile={profile} />
                  <SavingsGoalCard
                    savingsTargetAmount={myPage.savingsTargetAmount}
                    onSaved={handleSavingsGoalSaved}
                  />
                </div>
              )}

              {activeTab === "cards" && (
                <div className="mp-tab-panel">
                  <CardConnectionCard cardConnections={cardConnections} />
                </div>
              )}

              {activeTab === "account" && (
                <div className="mp-tab-panel mp-split">
                  <AccountSecurityCard
                    onOpenChangePassword={() => setIsChangingPassword(true)}
                    onLogoutAllDevices={handleLogoutAllDevices}
                  />
                  <TermsHistoryCard termsAgreements={myPage.termsAgreements} />
                </div>
              )}

              {activeTab === "soon" && (
                <div className="mp-tab-panel mp-bookmark-tab">
                  <PolicyBookmarksCard />
                  <ComingSoonCard />
                </div>
              )}
            </>
          )
        )}

        {isChangingPassword && (
          <ChangePasswordModal
            onClose={() => setIsChangingPassword(false)}
            onChanged={handlePasswordChanged}
          />
        )}

        {toast && (
          <div className="mp-toast show" role="status" aria-live="polite">
            {toast}
          </div>
        )}
      </main>
    </div>
  );
}

export default MyPage;
