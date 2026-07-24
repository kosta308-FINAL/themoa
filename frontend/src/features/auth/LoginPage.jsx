import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { kakaoLoginUrl, login as requestLogin } from "../../api/authApi";
import { useAuth } from "../../hooks/useAuth";
import { getApiErrorMessage } from "../../utils/apiError";
import { isAdminAccessToken } from "../../utils/accessToken";
import AuthLayout from "./components/AuthLayout";

function LoginPage() {
  const [searchParams] = useSearchParams();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(
    searchParams.get("error") === "kakao"
      ? "카카오 로그인에 실패했어요. 다시 시도해 주세요."
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

      <p className="auth-switch">
        아직 계정이 없나요? <Link to="/signup">회원가입</Link>
      </p>
    </AuthLayout>
  );
}

export default LoginPage;
