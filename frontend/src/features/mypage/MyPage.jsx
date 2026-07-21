import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getApiErrorMessage } from "../../utils/apiError";
import { getMyPage } from "../../api/mypageApi";
import { getCardConnections } from "../../api/spendingGuideApi";
import { logoutAllDevices } from "../../api/authApi";
import { useAuth } from "../../hooks/useAuth";
import ProfileCard from "./components/ProfileCard";
import SavingsGoalCard from "./components/SavingsGoalCard";
import CardConnectionCard from "./components/CardConnectionCard";
import AccountSecurityCard from "./components/AccountSecurityCard";
import ChangePasswordModal from "./components/ChangePasswordModal";
import TermsHistoryCard from "./components/TermsHistoryCard";
import ComingSoonCard from "./components/ComingSoonCard";
import "./MyPage.css";

function MyPage() {
  const navigate = useNavigate();
  const { logout } = useAuth();

  const [myPage, setMyPage] = useState(null);
  const [cardConnections, setCardConnections] = useState(null);
  const [pageError, setPageError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [toast, setToast] = useState("");

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

  return (
    <main className="mp-page">
      <div className="mp-page-head">
        <div>
          <h1>마이페이지</h1>
          <p>회원 정보, 저축목표, 카드 연동, 계정 관리를 한곳에서 확인해요.</p>
        </div>
      </div>

      {pageError && (
        <div className="mp-page-error">
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
          <div className="mp-grid">
            <ProfileCard profile={myPage.profile} />
            <SavingsGoalCard
              savingsTargetAmount={myPage.savingsTargetAmount}
              onSaved={handleSavingsGoalSaved}
            />
            <CardConnectionCard cardConnections={cardConnections} />
            <AccountSecurityCard
              onOpenChangePassword={() => setIsChangingPassword(true)}
              onLogoutAllDevices={handleLogoutAllDevices}
            />
            <TermsHistoryCard termsAgreements={myPage.termsAgreements} />
            <ComingSoonCard />
          </div>
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
  );
}

export default MyPage;
