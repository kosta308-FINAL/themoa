import BrandLogo from "../../../components/common/BrandLogo";
import "../Auth.css";

/**
 * 로그인·회원가입 공용 스플릿 레이아웃.
 * 왼쪽 딥그린 패널에 대시보드의 핵심 오브젝트("오늘 쓸 수 있는 금액" 카드)를 실물로 보여주고,
 * 오른쪽에 폼을 둔다. 모바일에서는 패널이 로고만 남는 상단 바로 줄어든다.
 */
function AuthLayout({ children }) {
  const dateLabel = new Date().toLocaleDateString("ko-KR", {
    month: "long",
    day: "numeric",
    weekday: "long",
  });

  return (
    <div className="auth">
      <aside className="auth-panel">
        <BrandLogo
          to="/"
          label="themoa"
          size="large"
          variant="auth"
          className="auth-logo"
          ariaLabel="themoa 홈으로 이동"
        />
        <div className="auth-panel-body">
          <p className="auth-panel-date">{dateLabel}</p>
          <h1 className="auth-panel-title">
            오늘 쓸 수 있는 금액부터
            <br />
            알고 시작하는 하루
          </h1>
          <div className="auth-demo-card" aria-hidden="true">
            <span className="auth-demo-label">오늘 쓸 수 있는 금액</span>
            <strong className="auth-demo-amount">₩ 47,200</strong>
            <div className="auth-demo-bar">
              <span className="auth-demo-fill" style={{ width: "67%" }} />
            </div>
            <span className="auth-demo-note">
              이번 달 저축 목표의 67% 달성 중
            </span>
          </div>
        </div>
        <p className="auth-panel-foot">
          월급과 저축 목표로 하루 예산을 역산하는 소비 가이드
        </p>
      </aside>
      <main className="auth-content">
        <div className="auth-form-area">{children}</div>
      </main>
    </div>
  );
}

export default AuthLayout;
