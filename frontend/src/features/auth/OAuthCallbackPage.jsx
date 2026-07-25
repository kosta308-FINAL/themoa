import { useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { exchangeOAuthCode } from "../../api/authApi";
import { useAuth } from "../../hooks/useAuth";
import { getApiErrorMessage } from "../../utils/apiError";
import { isAdminAccessToken } from "../../utils/accessToken";
import AuthLayout from "./components/AuthLayout";

/**
 * 소셜(카카오·구글) 콜백(SocialLoginSuccessHandler)이 리다이렉트하는 착지 페이지.
 * 교환코드를 1회 소비해 기존 회원 로그인 또는 신규가입 분기를 이어받는다(auth.md §6·§8).
 */
function OAuthCallbackPage() {
  const [searchParams] = useSearchParams();
  const code = searchParams.get("code");
  const { login } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState("");
  const requested = useRef(false);

  useEffect(() => {
    if (!code || requested.current) return;
    requested.current = true;

    exchangeOAuthCode(code)
      .then((res) => {
        const data = res.data.data;
        if (data.requiresSignup) {
          navigate("/oauth/signup", {
            replace: true,
            state: {
              signupTicket: data.signupTicket,
              nickname: data.nickname,
              email: data.email,
            },
          });
          return;
        }
        login(data.token);
        navigate(
          isAdminAccessToken() ? "/admin/customer-service" : "/dashboard",
          { replace: true },
        );
      })
      .catch((err) => {
        setError(
          getApiErrorMessage(
            err,
            "소셜 로그인 처리 중 문제가 생겼어요. 다시 시도해 주세요.",
          ),
        );
      });
  }, [code, login, navigate]);

  const displayError =
    error || (!code ? "소셜 로그인 정보를 확인하지 못했어요." : "");

  return (
    <AuthLayout>
      <div className="auth-head">
        <h2 className="auth-title">소셜 로그인</h2>
      </div>
      {displayError ? (
        <>
          <p className="auth-error" role="alert">
            {displayError}
          </p>
          <button
            type="button"
            className="auth-submit"
            onClick={() => navigate("/login", { replace: true })}
          >
            로그인으로 돌아가기
          </button>
        </>
      ) : (
        <p className="auth-sub">로그인을 처리하고 있어요…</p>
      )}
    </AuthLayout>
  );
}

export default OAuthCallbackPage;
