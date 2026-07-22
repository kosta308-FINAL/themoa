import { useState } from "react";
import { Link } from "react-router-dom";
import { findEmail } from "../../api/authApi";
import CalendarPopover from "../../components/common/CalendarPopover";
import DashboardIcon from "../../components/common/DashboardIcon";
import { getApiErrorMessage } from "../../utils/apiError";
import AuthLayout from "./components/AuthLayout";

const formatBirthDate = (iso) => {
  const [year, month, day] = iso.split("-");
  return `${year}년 ${Number(month)}월 ${Number(day)}일`;
};

function FindIdPage() {
  const [nickname, setNickname] = useState("");
  const [birthDate, setBirthDate] = useState("");
  const [isBirthDateOpen, setIsBirthDateOpen] = useState(false);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [maskedEmail, setMaskedEmail] = useState("");

  const today = new Date().toISOString().slice(0, 10);
  const birthYearFloor = Number(today.slice(0, 4)) - 100;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (submitting) return;
    if (!nickname.trim() || !birthDate) {
      setError("닉네임과 생년월일을 모두 입력해 주세요.");
      return;
    }
    setError("");
    setSubmitting(true);
    try {
      const res = await findEmail(nickname.trim(), birthDate);
      setMaskedEmail(res.data.data.maskedEmail);
    } catch (err) {
      setMaskedEmail("");
      setError(getApiErrorMessage(err, "일치하는 계정을 찾을 수 없습니다."));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout>
      <div className="auth-head">
        <h2 className="auth-title">아이디를 찾아드릴게요</h2>
        <p className="auth-sub">
          가입할 때 입력한 닉네임과 생년월일을 입력해 주세요.
        </p>
      </div>

      {maskedEmail ? (
        <div className="auth-result">
          <span className="auth-result-label">가입하신 이메일이에요</span>
          <strong className="auth-result-value">{maskedEmail}</strong>
          <Link to="/login" className="auth-submit auth-result-cta">
            로그인하러 가기
          </Link>
        </div>
      ) : (
        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          {error && (
            <p className="auth-error" role="alert">
              {error}
            </p>
          )}

          <label className="auth-field">
            <span className="auth-field-label">닉네임</span>
            <input
              className="auth-input"
              type="text"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              placeholder="가입 시 입력한 닉네임"
              maxLength={30}
              required
            />
          </label>

          <div className="auth-field">
            <span className="auth-field-label">생년월일</span>
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
                  title="생년월일 선택"
                  placement="top"
                  onSelect={(date) => {
                    setBirthDate(date);
                    setIsBirthDateOpen(false);
                  }}
                  onClose={() => setIsBirthDateOpen(false)}
                />
              )}
            </div>
          </div>

          <button type="submit" className="auth-submit" disabled={submitting}>
            {submitting ? "찾는 중…" : "아이디 찾기"}
          </button>
        </form>
      )}

      <p className="auth-switch">
        비밀번호가 기억나지 않나요?{" "}
        <Link to="/find-password">비밀번호 찾기</Link>
      </p>
      <p className="auth-switch">
        <Link to="/login">로그인으로 돌아가기</Link>
      </p>
    </AuthLayout>
  );
}

export default FindIdPage;
