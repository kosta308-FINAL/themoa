import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import {
  googleLoginUrl,
  kakaoLoginUrl,
  login as requestLogin,
} from "../../api/authApi";
import { useAuth } from "../../hooks/useAuth";
import { getApiErrorMessage } from "../../utils/apiError";
import { isAdminAccessToken } from "../../utils/accessToken";
import AuthLayout from "./components/AuthLayout";

function LoginPage() {
  const [searchParams] = useSearchParams();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const oauthError = searchParams.get("error");
  const [error, setError] = useState(
    oauthError === "kakao"
      ? "카카오 로그인에 실패했어요. 다시 시도해 주세요."
      : oauthError === "google"
        ? "구글 로그인에 실패했어요. 다시 시도해 주세요."
        : "",
  );
  const [submitting, setSubmitting] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (submitting) return;
    setError("");
    setSubmitting(true);
    try {
      const res = await requestLogin(email.trim(), password);
      login(res.data.data);
      navigate(
        isAdminAccessToken() ? "/admin/customer-service" : "/dashboard",
        { replace: true },
      );
    } catch (err) {
      setError(
        getApiErrorMessage(err, "이메일 또는 비밀번호가 올바르지 않습니다."),
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout>
      <div className="auth-head">
        <h2 className="auth-title">다시 만나서 반가워요</h2>
        <p className="auth-sub">로그인하면 오늘의 소비 가이드가 준비돼요.</p>
      </div>

      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        {error && (
          <p className="auth-error" role="alert">
            {error}
          </p>
        )}

        <label className="auth-field">
          <span className="auth-field-label">이메일</span>
          <input
            className="auth-input"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com"
            autoComplete="email"
            required
          />
        </label>

        <label className="auth-field">
          <span className="auth-field-label">비밀번호</span>
          <input
            className="auth-input"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="비밀번호"
            autoComplete="current-password"
            required
          />
        </label>

        <div className="auth-find-links">
          <Link to="/find-email">아이디 찾기</Link>
          <span className="auth-find-divider" aria-hidden="true" />
          <Link to="/find-password">비밀번호 찾기</Link>
        </div>

        <button type="submit" className="auth-submit" disabled={submitting}>
          {submitting ? "로그인 중…" : "로그인"}
        </button>
      </form>

      <div className="auth-divider">
        <span>또는</span>
      </div>

      <button
        type="button"
        className="auth-kakao-btn"
        onClick={() => {
          window.location.href = kakaoLoginUrl();
        }}
      >
        <svg className="auth-kakao-icon" viewBox="0 0 24 24" aria-hidden="true">
          <path
            fill="#191600"
            d="M12 3C6.98 3 2.9 6.24 2.9 10.24c0 2.56 1.68 4.81 4.22 6.1-.18.66-.68 2.5-.78 2.9-.12.5.18.49.39.36.16-.1 2.6-1.77 3.66-2.49.53.08 1.07.11 1.61.11 5.02 0 9.1-3.24 9.1-7.24C21.1 6.24 17.02 3 12 3Z"
          />
        </svg>
        카카오로 로그인
      </button>

      <button
        type="button"
        className="auth-google-btn"
        onClick={() => {
          window.location.href = googleLoginUrl();
        }}
      >
        <svg
          className="auth-google-icon"
          viewBox="0 0 48 48"
          aria-hidden="true"
        >
          <path
            fill="#FFC107"
            d="M43.6 20.5H42V20.4H24v7.2h11.3C33.7 32 29.3 35 24 35c-6.6 0-12-5.4-12-12s5.4-12 12-12c3.1 0 5.9 1.2 8 3l5.1-5.1C33.9 5.9 29.2 4 24 4 12.9 4 4 12.9 4 24s8.9 20 20 20 20-8.9 20-20c0-1.2-.1-2.4-.4-3.5Z"
          />
          <path
            fill="#FF3D00"
            d="m6.3 14.7 5.9 4.3C13.8 15.4 18.5 12.4 24 12.4c3.1 0 5.9 1.2 8 3l5.1-5.1C33.9 5.9 29.2 4 24 4c-7.4 0-13.8 4.2-17.1 10.4Z"
          />
          <path
            fill="#4CAF50"
            d="M24 44c5.2 0 9.9-1.7 13.5-4.6l-6.2-5.3c-2 1.4-4.5 2.2-7.3 2.2-5.3 0-9.6-3.4-11.3-8.1l-6.1 4.7C10.1 39.6 16.5 44 24 44Z"
          />
          <path
            fill="#1976D2"
            d="M43.6 20.5H42V20.4H24v7.2h11.3c-.8 2.2-2.2 4.1-4.1 5.5l6.2 5.3C41.4 34.8 44 29.9 44 24c0-1.2-.1-2.4-.4-3.5Z"
          />
        </svg>
        구글로 로그인
      </button>

      <p className="auth-switch">
        아직 계정이 없나요? <Link to="/signup">회원가입</Link>
      </p>
    </AuthLayout>
  );
}

export default LoginPage;
