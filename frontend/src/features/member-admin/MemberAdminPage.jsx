import { useEffect, useState } from "react";
import AdminLayout from "../../components/layout/AdminLayout";
import MemberDetailDrawer from "./components/MemberDetailDrawer";
import { getAdminMembers } from "../../api/memberAdminApi";
import { getApiErrorMessage } from "../../utils/apiError";
import "./MemberAdminPage.css";

const GENDER_LABEL = { MALE: "남성", FEMALE: "여성" };
const ENTRY_MODE_LABEL = { MANUAL: "수기", CARD: "카드 연동" };
const STATUS_LABEL = { ACTIVE: "활성", LOCKED: "잠금", WITHDRAWN: "탈퇴" };
const STATUS_BADGE_CLASS = {
  ACTIVE: "green",
  LOCKED: "yellow",
  WITHDRAWN: "gray",
};

function formatDate(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(
    date.getDate(),
  ).padStart(2, "0")}`;
}

function MemberAdminPage() {
  const [items, setItems] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [email, setEmail] = useState("");
  const [memberId, setMemberId] = useState("");
  const [lockedOnly, setLockedOnly] = useState(false);
  const [includeWithdrawn, setIncludeWithdrawn] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedMemberId, setSelectedMemberId] = useState(null);

  const load = async () => {
    setIsLoading(true);
    setError("");
    try {
      const data = await getAdminMembers({
        email: email || undefined,
        memberId: memberId || undefined,
        lockedOnly,
        includeWithdrawn,
        size: 50,
      });
      setItems(data?.content || []);
      setTotalElements(data?.totalElements || 0);
    } catch (requestError) {
      setError(
        getApiErrorMessage(requestError, "회원 목록을 불러오지 못했어요."),
      );
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    const timer = window.setTimeout(load, 250);
    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [email, memberId, lockedOnly, includeWithdrawn]);

  const handleDrawerClose = (changed) => {
    setSelectedMemberId(null);
    if (changed) {
      load();
    }
  };

  return (
    <AdminLayout
      title="회원 관리"
      subtitle="회원 조회, 계정 잠금/해제, 강제 로그아웃, 강제 탈퇴를 제공합니다. 조회·조치 이력은 모두 감사 로그로 남습니다."
    >
      <div className="mba-page">
        <section className="mba-filters">
          <input
            type="text"
            className="mba-input"
            placeholder="이메일 (정확히 일치)"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <input
            type="number"
            className="mba-input mba-input-narrow"
            placeholder="회원 ID"
            value={memberId}
            onChange={(e) => setMemberId(e.target.value)}
          />
          <label className="mba-checkbox">
            <input
              type="checkbox"
              checked={lockedOnly}
              onChange={(e) => setLockedOnly(e.target.checked)}
            />
            잠금 계정만
          </label>
          <label className="mba-checkbox">
            <input
              type="checkbox"
              checked={includeWithdrawn}
              onChange={(e) => setIncludeWithdrawn(e.target.checked)}
            />
            탈퇴 회원 포함
          </label>
        </section>

        <section className="mba-panel">
          <div className="mba-panel-header">
            <div>
              <div className="mba-panel-title">회원 목록</div>
              <div className="mba-panel-sub">
                이메일은 마스킹되어 표시됩니다. 클릭하면 상세를 볼 수 있어요(상세
                열람은 감사 로그에 기록됩니다).
              </div>
            </div>
          </div>
          {error && <div className="mba-alert">{error}</div>}
          {isLoading ? (
            <div className="mba-empty">불러오는 중...</div>
          ) : items.length === 0 ? (
            <div className="mba-empty">조건에 맞는 회원이 없습니다.</div>
          ) : (
            <table className="mba-table">
              <thead>
                <tr>
                  <th>회원 ID</th>
                  <th>이메일</th>
                  <th>닉네임</th>
                  <th>성별</th>
                  <th>가입 경로</th>
                  <th>상태</th>
                  <th>가입일</th>
                  <th>최근 활동일</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr
                    key={item.id}
                    className="mba-row-clickable"
                    onClick={() => setSelectedMemberId(item.id)}
                  >
                    <td>{item.id}</td>
                    <td>{item.maskedEmail}</td>
                    <td>{item.name}</td>
                    <td>{GENDER_LABEL[item.gender] || item.gender}</td>
                    <td>{ENTRY_MODE_LABEL[item.entryMode] || item.entryMode}</td>
                    <td>
                      <span
                        className={`mba-badge ${STATUS_BADGE_CLASS[item.status] || "gray"}`}
                      >
                        {STATUS_LABEL[item.status] || item.status}
                      </span>
                    </td>
                    <td>{formatDate(item.createdAt)}</td>
                    <td>{formatDate(item.lastActiveAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <div className="mba-total-count">총 {totalElements}명</div>
        </section>

        {selectedMemberId && (
          <MemberDetailDrawer
            memberId={selectedMemberId}
            onClose={handleDrawerClose}
          />
        )}
      </div>
    </AdminLayout>
  );
}

export default MemberAdminPage;
