import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { sendEmailCode, signup, verifyEmailCode } from "../../api/authApi";
import CalendarPopover from "../../components/common/CalendarPopover";
import DashboardIcon from "../../components/common/DashboardIcon";
import { useAuth } from "../../hooks/useAuth";
import { getApiErrorMessage } from "../../utils/apiError";
import AuthLayout from "./components/AuthLayout";
import TermsAgreement from "./components/TermsAgreement";
import { REQUIRED_TERMS_KEYS } from "./constants/terms";

const formatBirthDate = (iso) => {
  const [year, month, day] = iso.split("-");
  return `${year}년 ${Number(month)}월 ${Number(day)}일`;
};

/* 서버(SignupRequest)와 동일한 규칙: 공백 없이 10~64자, 영문·숫자·특수문자 모두 포함 */
const PASSWORD_RULE = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9\s])\S{10,64}$/;
const CODE_TTL_SECONDS = 300;
const RESEND_COOLDOWN_SECONDS = 60;

const formatTtl = (seconds) => {
  const m = String(Math.floor(seconds / 60)).padStart(2, "0");
  const s = String(seconds % 60).padStart(2, "0");
  return `${m}:${s}`;
};

function SignupPage() {
  const [step, setStep] = useState(1);
  const [error, setError] = useState("");
  const { login } = useAuth();
  const navigate = useNavigate();

  // step 1 — 약관 동의
  const [agreements, setAgreements] = useState({});

  // step 2 — 이메일 인증
  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [codeSent, setCodeSent] = useState(false);
  const [codeTtl, setCodeTtl] = useState(0);
  const [resendLeft, setResendLeft] = useState(0);
  const [sending, setSending] = useState(false);
  const [verifying, setVerifying] = useState(false);

  // step 3 — 기본 정보
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [nickname, setNickname] = useState("");
  const [gender, setGender] = useState("");
  const [birthDate, setBirthDate] = useState("");
  const [isBirthDateOpen, setIsBirthDateOpen] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  const normalizedEmail = email.trim().toLowerCase();

  useEffect(() => {
    if (!codeSent || step !== 2) return;
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
      await sendEmailCode(normalizedEmail);
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
      await verifyEmailCode(normalizedEmail, code.trim());
      setStep(3);
    } catch (err) {
      setError(
        getApiErrorMessage(err, "인증 코드가 올바르지 않거나 만료되었습니다."),
      );
    } finally {
      setVerifying(false);
    }
  };

  const validateStep2 = () => {
    const errors = {};
    if (!PASSWORD_RULE.test(password)) {
      errors.password =
        "공백 없이 10~64자, 영문·숫자·특수문자를 모두 포함해야 해요.";
    }
    if (password !== passwordConfirm) {
      errors.passwordConfirm = "비밀번호가 서로 일치하지 않아요.";
    }
    if (!nickname.trim()) {
      errors.nickname = "닉네임을 입력해 주세요.";
    } else if (nickname.trim().length > 30) {
      errors.nickname = "닉네임은 30자 이하여야 해요.";
    }
    if (!gender) {
      errors.gender = "성별을 선택해 주세요.";
    }
    if (!birthDate) {
      errors.birthDate = "출생일을 입력해 주세요.";
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleAgreementChange = (key, value) => {
    setAgreements((prev) => ({ ...prev, [key]: value }));
  };

  const handleContinueToEmail = (e) => {
    e.preventDefault();
    const missingRequired = REQUIRED_TERMS_KEYS.some((key) => !agreements[key]);
    if (missingRequired) {
      setError("필수 약관에 모두 동의해야 다음으로 진행할 수 있어요.");
      return;
    }
    setError("");
    setStep(2);
  };

  const handleSignup = async (e) => {
    e.preventDefault();
    if (submitting) return;
    setError("");
    if (!validateStep2()) return;
    setSubmitting(true);
    try {
      const res = await signup({
        email: normalizedEmail,
        password,
        passwordConfirm,
        nickname: nickname.trim(),
        gender,
        birthDate,
        agreedServiceTerms: !!agreements.service,
        agreedPrivacyPolicy: !!agreements.privacy,
        agreedDataCollection: !!agreements.dataCollection,
      });
      login(res.data.data);
      navigate("/dashboard");
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          "가입을 완료하지 못했어요. 잠시 후 다시 시도해 주세요.",
        ),
      );
    } finally {
      setSubmitting(false);
    }
  };

  const today = new Date().toISOString().slice(0, 10);
  const birthYearFloor = Number(today.slice(0, 4)) - 100;

  return (
    <AuthLayout>
      <div className="auth-head">
        <h2 className="auth-title">3분이면 시작할 수 있어요</h2>
        <p className="auth-sub">
          약관 동의 후 이메일 인증과 기본 정보만 입력하면 가입이 끝나요.
        </p>
      </div>

      <ol className="auth-steps">
        <li className={step === 1 ? "current" : "done"}>
          <span className="auth-step-num">{step > 1 ? "✓" : "1"}</span>
          약관 동의
        </li>
        <li className={step === 2 ? "current" : step > 2 ? "done" : ""}>
          <span className="auth-step-num">{step > 2 ? "✓" : "2"}</span>
          이메일 인증
        </li>
        <li className={step === 3 ? "current" : ""}>
          <span className="auth-step-num">3</span>
          기본 정보
        </li>
      </ol>

      {step === 1 && (
        <form className="auth-form" onSubmit={handleContinueToEmail} noValidate>
          {error && (
            <p className="auth-error" role="alert">
              {error}
            </p>
          )}

          <TermsAgreement
            agreements={agreements}
            onChange={handleAgreementChange}
          />

          <button type="submit" className="auth-submit">
            다음
          </button>
        </form>
      )}

      {step === 2 && (
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
                로그인에 사용할 이메일이에요. 인증 코드를 보내 확인해요.
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

      {step === 3 && (
        <form className="auth-form" onSubmit={handleSignup} noValidate>
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
            <span className="auth-field-label">비밀번호</span>
            <input
              className="auth-input"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="비밀번호"
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
            <span className="auth-field-label">비밀번호 확인</span>
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

          <label className="auth-field">
            <span className="auth-field-label">닉네임</span>
            <input
              className="auth-input"
              type="text"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              placeholder="더모아에서 쓸 이름"
              maxLength={30}
              autoComplete="nickname"
              required
            />
            {fieldErrors.nickname && (
              <span className="auth-field-error">{fieldErrors.nickname}</span>
            )}
          </label>

          <div className="auth-field">
            <span className="auth-field-label">성별</span>
            <div className="auth-segment" role="radiogroup" aria-label="성별">
              <label className={gender === "MALE" ? "on" : ""}>
                <input
                  type="radio"
                  name="gender"
                  value="MALE"
                  checked={gender === "MALE"}
                  onChange={() => setGender("MALE")}
                />
                남
              </label>
              <label className={gender === "FEMALE" ? "on" : ""}>
                <input
                  type="radio"
                  name="gender"
                  value="FEMALE"
                  checked={gender === "FEMALE"}
                  onChange={() => setGender("FEMALE")}
                />
                여
              </label>
            </div>
            {fieldErrors.gender && (
              <span className="auth-field-error">{fieldErrors.gender}</span>
            )}
          </div>

          <div className="auth-field">
            <span className="auth-field-label">출생일</span>
            <div className="auth-date-field">
              <button
                type="button"
                className="auth-date-trigger"
                onClick={() => setIsBirthDateOpen((open) => !open)}
              >
                <DashboardIcon name="calendar" size={16} />
                <span className={birthDate ? "" : "auth-date-placeholder"}>
                  {birthDate ? formatBirthDate(birthDate) : "연도-월-일"}
                </span>
              </button>
              {isBirthDateOpen && (
                <CalendarPopover
                  value={birthDate}
                  max={today}
                  minYear={birthYearFloor}
                  title="출생일 선택"
                  placement="top"
                  onSelect={(date) => {
                    setBirthDate(date);
                    setIsBirthDateOpen(false);
                  }}
                  onClose={() => setIsBirthDateOpen(false)}
                />
              )}
            </div>
            {fieldErrors.birthDate ? (
              <span className="auth-field-error">{fieldErrors.birthDate}</span>
            ) : (
              <span className="auth-hint">
                만 19세 이상만 가입할 수 있어요.
              </span>
            )}
          </div>

          <button type="submit" className="auth-submit" disabled={submitting}>
            {submitting ? "가입 중…" : "가입하고 시작하기"}
          </button>
        </form>
      )}

      <p className="auth-switch">
        이미 계정이 있나요? <Link to="/login">로그인</Link>
      </p>
    </AuthLayout>
  );
}

export default SignupPage;
