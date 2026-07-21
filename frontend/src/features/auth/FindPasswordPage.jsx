import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  resetPassword,
  sendPasswordResetCode,
  verifyPasswordResetCode,
} from "../../api/authApi";
import DashboardIcon from "../../components/common/DashboardIcon";
import { getApiErrorMessage } from "../../utils/apiError";
import AuthLayout from "./components/AuthLayout";

/* 서버(PasswordResetRequest)와 동일한 규칙: 공백 없이 10~64자, 영문·숫자·특수문자 모두 포함 */
const PASSWORD_RULE = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9\s])\S{10,64}$/;
const CODE_TTL_SECONDS = 300;
const RESEND_COOLDOWN_SECONDS = 60;

const formatTtl = (seconds) => {
  const m = String(Math.floor(seconds / 60)).padStart(2, "0");
  const s = String(seconds % 60).padStart(2, "0");
  return `${m}:${s}`;
};

function FindPasswordPage() {
  const [step, setStep] = useState(1);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  // step 1 — 이메일 인증
  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [codeSent, setCodeSent] = useState(false);
  const [codeTtl, setCodeTtl] = useState(0);
  const [resendLeft, setResendLeft] = useState(0);
  const [sending, setSending] = useState(false);
  const [verifying, setVerifying] = useState(false);

  // step 2 — 새 비밀번호
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);

  const normalizedEmail = email.trim().toLowerCase();

  useEffect(() => {
    if (!codeSent || step !== 1) return;
    const id = setInterval(() => {
      setCodeTtl((s) => (s > 0 ? s - 1 : 0));
      setResendLeft((s) => (s > 0 ? s - 1 : 0));
    }, 1000);
    return () => clearInterval(id);
  }, [codeSent, step]);

  const handleSendCode = async () => {
    if (sending || resendLeft > 0) return;
    if (!normalizedEmail) {
      setError("이메일을 입력해 주세요.");
      return;
    }
    setError("");
    setSending(true);
    try {
      await sendPasswordResetCode(normalizedEmail);
      setCodeSent(true);
      setCode("");
      setCodeTtl(CODE_TTL_SECONDS);
      setResendLeft(RESEND_COOLDOWN_SECONDS);
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          "인증 메일을 보내지 못했어요. 잠시 후 다시 시도해 주세요.",
        ),
      );
    } finally {
      setSending(false);
    }
  };

  const handleVerifyCode = async (e) => {
    e.preventDefault();
    if (verifying) return;
    setError("");
    setVerifying(true);
    try {
      await verifyPasswordResetCode(normalizedEmail, code.trim());
      setStep(2);
    } catch (err) {
      setError(
        getApiErrorMessage(err, "인증 코드가 올바르지 않거나 만료되었습니다."),
      );
    } finally {
      setVerifying(false);
    }
  };

  const validatePassword = () => {
    const errors = {};
    if (!PASSWORD_RULE.test(password)) {
      errors.password =
        "공백 없이 10~64자, 영문·숫자·특수문자를 모두 포함해야 해요.";
    }
    if (password !== passwordConfirm) {
      errors.passwordConfirm = "비밀번호가 서로 일치하지 않아요.";
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    if (submitting) return;
    setError("");
    if (!validatePassword()) return;
    setSubmitting(true);
    try {
      await resetPassword({
        email: normalizedEmail,
        newPassword: password,
        newPasswordConfirm: passwordConfirm,
      });
      setDone(true);
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          "비밀번호를 변경하지 못했어요. 잠시 후 다시 시도해 주세요.",
        ),
      );
    } finally {
      setSubmitting(false);
    }
  };

  if (done) {
    return (
      <AuthLayout>
        <div className="auth-head">
          <h2 className="auth-title">비밀번호가 변경됐어요</h2>
          <p className="auth-sub">새 비밀번호로 다시 로그인해 주세요.</p>
        </div>
        <button
          type="button"
          className="auth-submit"
          onClick={() => navigate("/login", { replace: true })}
        >
          로그인하러 가기
        </button>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout>
      <div className="auth-head">
        <h2 className="auth-title">비밀번호를 재설정해요</h2>
        <p className="auth-sub">
          가입하신 이메일로 인증 코드를 보내드려요. 인증 후 새 비밀번호를 설정할
          수 있어요.
        </p>
      </div>

      {step > 1 && (
        <div className="auth-steps-row">
          <button
            type="button"
            className="auth-back"
            aria-label="이전 단계로"
            onClick={() => {
              setError("");
              setStep((s) => s - 1);
            }}
          >
            <DashboardIcon name="chevron-left" size={16} />
          </button>
        </div>
      )}

      {step === 1 && (
        <form className="auth-form" onSubmit={handleVerifyCode} noValidate>
          {error && (
            <p className="auth-error" role="alert">
              {error}
            </p>
          )}

          <div className="auth-field">
            <span className="auth-field-label">이메일</span>
            <div className="auth-row">
              <input
                className="auth-input"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
                autoComplete="email"
                disabled={codeSent}
                required
              />
              {!codeSent && (
                <button
                  type="button"
                  className="auth-btn-secondary"
                  onClick={handleSendCode}
                  disabled={sending || !normalizedEmail}
                >
                  {sending ? "보내는 중…" : "인증 코드 보내기"}
                </button>
              )}
            </div>
            {!codeSent && (
              <span className="auth-hint">
                가입할 때 사용한 이메일을 입력해 주세요.
              </span>
            )}
          </div>

          {codeSent && (
            <>
              <label className="auth-field">
                <span className="auth-field-label">인증 코드</span>
                <input
                  className="auth-input"
                  type="text"
                  inputMode="numeric"
                  maxLength={6}
                  value={code}
                  onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))}
                  placeholder="메일로 받은 6자리 숫자"
                  autoComplete="one-time-code"
                  required
                />
                <span className="auth-code-meta">
                  <span
                    className={`auth-code-ttl${codeTtl === 0 ? " expired" : ""}`}
                  >
                    {codeTtl > 0
                      ? `남은 시간 ${formatTtl(codeTtl)}`
                      : "코드가 만료됐어요. 다시 보내 주세요."}
                  </span>
                  <button
                    type="button"
                    className="auth-resend"
                    onClick={handleSendCode}
                    disabled={sending || resendLeft > 0}
                  >
                    {resendLeft > 0
                      ? `다시 보내기 (${resendLeft}초 후)`
                      : "인증 코드 다시 보내기"}
                  </button>
                </span>
              </label>

              <button
                type="submit"
                className="auth-submit"
                disabled={verifying || code.length !== 6}
              >
                {verifying ? "확인 중…" : "코드 확인"}
              </button>
            </>
          )}
        </form>
      )}

      {step === 2 && (
        <form className="auth-form" onSubmit={handleResetPassword} noValidate>
          {error && (
            <p className="auth-error" role="alert">
              {error}
            </p>
          )}

          <div className="auth-verified">
            <span>{normalizedEmail}</span>
            <span className="auth-verified-badge">인증 완료</span>
          </div>

          <label className="auth-field">
            <span className="auth-field-label">새 비밀번호</span>
            <input
              className="auth-input"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="새 비밀번호"
              autoComplete="new-password"
              required
            />
            {fieldErrors.password ? (
              <span className="auth-field-error">{fieldErrors.password}</span>
            ) : (
              <span className="auth-hint">
                공백 없이 10~64자, 영문·숫자·특수문자 모두 포함
              </span>
            )}
          </label>

          <label className="auth-field">
            <span className="auth-field-label">새 비밀번호 확인</span>
            <input
              className="auth-input"
              type="password"
              value={passwordConfirm}
              onChange={(e) => setPasswordConfirm(e.target.value)}
              placeholder="비밀번호를 한 번 더 입력"
              autoComplete="new-password"
              required
            />
            {fieldErrors.passwordConfirm && (
              <span className="auth-field-error">
                {fieldErrors.passwordConfirm}
              </span>
            )}
          </label>

          <button type="submit" className="auth-submit" disabled={submitting}>
            {submitting ? "변경 중…" : "비밀번호 변경하기"}
          </button>
        </form>
      )}

      <p className="auth-switch">
        <Link to="/login">로그인으로 돌아가기</Link>
      </p>
    </AuthLayout>
  );
}

export default FindPasswordPage;
