import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { login as requestLogin } from "../../api/authApi";
import { useAuth } from "../../hooks/useAuth";
import { getApiErrorMessage } from "../../utils/apiError";
import { isAdminAccessToken } from "../../utils/accessToken";
import AuthLayout from "./components/AuthLayout";

function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
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

        <button type="submit" className="auth-submit" disabled={submitting}>
          {submitting ? "로그인 중…" : "로그인"}
        </button>
      </form>

      <p className="auth-switch">
        아직 계정이 없나요? <Link to="/signup">회원가입</Link>
      </p>
    </AuthLayout>
  );
}

export default LoginPage;
