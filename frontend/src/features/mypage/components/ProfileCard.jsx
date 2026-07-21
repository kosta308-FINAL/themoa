import DashboardIcon from "../../../components/common/DashboardIcon";
import {
  ENTRY_MODE_LABELS,
  GENDER_LABELS,
  INCOME_TYPE_LABELS,
  formatDate,
  formatWon,
} from "../mypageUtils";

function ProfileCard({ profile }) {
  const incomeLabel =
    INCOME_TYPE_LABELS[profile.incomeType] || profile.incomeType;
  const incomeValue =
    profile.incomeType === "HOURLY" ? profile.hourlyWage : profile.salaryAmount;

  return (
    <section className="mp-card">
      <div className="mp-card-head">
        <span className="mp-card-icon">
          <DashboardIcon name="user" size={17} />
        </span>
        <h2>회원 정보</h2>
      </div>
      <dl className="mp-info-list">
        <div>
          <dt>성별</dt>
          <dd>{GENDER_LABELS[profile.gender] || profile.gender}</dd>
        </div>
        <div>
          <dt>생년월일</dt>
          <dd>{formatDate(profile.birthDate)}</dd>
        </div>
        <div>
          <dt>가입일</dt>
          <dd>{formatDate(profile.createdAt)}</dd>
        </div>
        <div>
          <dt>입력 모드</dt>
          <dd>
            {ENTRY_MODE_LABELS[profile.entryMode] || profile.entryMode}
            {profile.entryMode === "CARD" &&
              (profile.cardSyncEnabled ? " · 자동수집 ON" : " · 자동수집 OFF")}
          </dd>
        </div>
        <div>
          <dt>소득유형</dt>
          <dd>{incomeLabel}</dd>
        </div>
        <div>
          <dt>{profile.incomeType === "HOURLY" ? "시급" : "월급"}</dt>
          <dd>{incomeValue != null ? formatWon(incomeValue) : "미설정"}</dd>
        </div>
        <div>
          <dt>급여일</dt>
          <dd>
            {profile.payday != null ? `매월 ${profile.payday}일` : "미설정"}
          </dd>
        </div>
      </dl>
    </section>
  );
}

export default ProfileCard;
