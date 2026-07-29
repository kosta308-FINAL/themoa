import { useCallback, useEffect, useState } from "react";
import {
  forceLogoutAdminMember,
  getAdminMemberActionLogs,
  getAdminMemberDetail,
  lockAdminMember,
  unlockAdminMember,
  withdrawAdminMember,
} from "../../../api/memberAdminApi";
import { getApiErrorMessage } from "../../../utils/apiError";

const GENDER_LABEL = { MALE: "남성", FEMALE: "여성" };
const ENTRY_MODE_LABEL = { MANUAL: "수기", CARD: "카드 연동" };
const STATUS_LABEL = { ACTIVE: "활성", LOCKED: "잠금", WITHDRAWN: "탈퇴" };
const STATUS_BADGE_CLASS = {
  ACTIVE: "green",
  LOCKED: "yellow",
  WITHDRAWN: "gray",
};
const ACTION_TYPE_LABEL = {
  VIEW_DETAIL: "상세 열람",
  LOCK: "계정 잠금",
  UNLOCK: "잠금 해제",
  FORCE_LOGOUT: "강제 로그아웃",
  FORCE_WITHDRAW: "강제 탈퇴",
};

const ACTIONS = {
  lock: {
    title: "계정을 잠글까요?",
    desc: "해제 전까지 로그인이 제한됩니다. 사유를 입력해 주세요.",
    requireReason: true,
    confirmLabel: "잠금",
    run: lockAdminMember,
  },
  unlock: {
    title: "잠금을 해제할까요?",
    desc: "사유는 선택 입력입니다.",
    requireReason: false,
    confirmLabel: "해제",
    run: unlockAdminMember,
  },
  forceLogout: {
    title: "강제 로그아웃할까요?",
    desc: "이 회원의 모든 기기 세션이 즉시 무효화됩니다. 사유는 선택 입력입니다.",
    requireReason: false,
    confirmLabel: "로그아웃",
    run: forceLogoutAdminMember,
  },
  withdraw: {
    title: "강제 탈퇴 처리할까요?",
    desc: "되돌릴 수 없습니다. 이메일·닉네임이 즉시 익명화됩니다. 사유를 입력해 주세요.",
    requireReason: true,
    confirmLabel: "탈퇴 처리",
    run: withdrawAdminMember,
  },
};

function formatDateTime(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(
    date.getDate(),
  ).padStart(2, "0")} ${String(date.getHours()).padStart(2, "0")}:${String(
    date.getMinutes(),
  ).padStart(2, "0")}`;
}

function MemberDetailDrawer({ memberId, onClose }) {
  const [detail, setDetail] = useState(null);
  const [actionLogs, setActionLogs] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [changed, setChanged] = useState(false);
  const [pendingActionKey, setPendingActionKey] = useState(null);
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState("");

  const load = useCallback(async () => {
    setIsLoading(true);
    setError("");
    try {
      const [detailData, logData] = await Promise.all([
        getAdminMemberDetail(memberId),
        getAdminMemberActionLogs(memberId, { size: 10 }),
      ]);
      setDetail(detailData);
      setActionLogs(logData?.content || []);
    } catch (requestError) {
      setError(
        getApiErrorMessage(requestError, "회원 상세를 불러오지 못했어요."),
      );
    } finally {
      setIsLoading(false);
    }
  }, [memberId]);

  useEffect(() => {
    const run = () => load();
    run();
  }, [load]);

  const openConfirm = (key) => {
    setActionError("");
    setReason("");
    setPendingActionKey(key);
  };

  const closeConfirm = () => {
    setPendingActionKey(null);
    setReason("");
    setActionError("");
  };

  const submitAction = async () => {
    const action = ACTIONS[pendingActionKey];
    if (action.requireReason && !reason.trim()) {
      setActionError("사유를 입력해 주세요.");
      return;
    }
    setSubmitting(true);
    setActionError("");
    try {
      await action.run(memberId, reason.trim() || undefined);
      setChanged(true);
      closeConfirm();
      await load();
    } catch (requestError) {
      setActionError(
        getApiErrorMessage(requestError, "조치를 처리하지 못했어요."),
      );
    } finally {
      setSubmitting(false);
    }
  };

  const pendingAction = pendingActionKey ? ACTIONS[pendingActionKey] : null;
  const status = detail?.status;

  return (
    <div
      className="mba-drawer-overlay"
      onClick={() => onClose(changed)}
    >
      <div className="mba-drawer" onClick={(e) => e.stopPropagation()}>
        <div className="mba-drawer-header">
          <h3>회원 상세</h3>
          <button
            type="button"
            className="mba-icon-btn"
            onClick={() => onClose(changed)}
          >
            ✕
          </button>
        </div>

        <div className="mba-drawer-body">
          {isLoading && <div className="mba-empty">불러오는 중...</div>}
          {error && <div className="mba-alert">{error}</div>}

          {detail && !isLoading && (
            <>
              <div className="mba-detail-meta">
                <span className={`mba-badge ${STATUS_BADGE_CLASS[status] || "gray"}`}>
                  {STATUS_LABEL[status] || status}
                </span>
                <span className="mba-muted">회원 ID {detail.id}</span>
              </div>

              <dl className="mba-detail-grid">
                <dt>이메일</dt>
                <dd>{detail.email}</dd>
                <dt>닉네임</dt>
                <dd>{detail.name}</dd>
                <dt>성별</dt>
                <dd>{GENDER_LABEL[detail.gender] || detail.gender}</dd>
                <dt>생년월일</dt>
                <dd>{detail.birthDate}</dd>
                <dt>가입 경로</dt>
                <dd>{ENTRY_MODE_LABEL[detail.entryMode] || detail.entryMode}</dd>
                <dt>카드 자동수집</dt>
                <dd>{detail.cardSyncEnabled ? "사용" : "중지"}</dd>
                <dt>연동 소셜 계정</dt>
                <dd>
                  {detail.linkedProviders?.length
                    ? detail.linkedProviders.join(", ")
                    : "없음"}
                </dd>
                <dt>로그인 실패 횟수</dt>
                <dd>{detail.loginFailCount}</dd>
                <dt>잠금 해제 시각</dt>
                <dd>{formatDateTime(detail.lockedUntil)}</dd>
                <dt>최근 활동일</dt>
                <dd>{formatDateTime(detail.lastActiveAt)}</dd>
                <dt>가입일</dt>
                <dd>{formatDateTime(detail.createdAt)}</dd>
                <dt>탈퇴일</dt>
                <dd>{formatDateTime(detail.withdrawnAt)}</dd>
              </dl>

              {status !== "WITHDRAWN" && (
                <div className="mba-actions">
                  {status === "LOCKED" ? (
                    <button
                      type="button"
                      className="mba-btn"
                      onClick={() => openConfirm("unlock")}
                    >
                      잠금 해제
                    </button>
                  ) : (
                    <button
                      type="button"
                      className="mba-btn"
                      onClick={() => openConfirm("lock")}
                    >
                      계정 잠금
                    </button>
                  )}
                  <button
                    type="button"
                    className="mba-btn"
                    onClick={() => openConfirm("forceLogout")}
                  >
                    강제 로그아웃
                  </button>
                  <button
                    type="button"
                    className="mba-btn mba-btn-danger"
                    onClick={() => openConfirm("withdraw")}
                  >
                    강제 탈퇴
                  </button>
                </div>
              )}

              <div className="mba-detail-label">관리자 조치 이력</div>
              {actionLogs.length === 0 ? (
                <div className="mba-empty">조치 이력이 없습니다.</div>
              ) : (
                <ul className="mba-action-log-list">
                  {actionLogs.map((log) => (
                    <li key={log.id} className="mba-action-log-item">
                      <div className="mba-action-log-head">
                        <span>{ACTION_TYPE_LABEL[log.actionType] || log.actionType}</span>
                        <span className="mba-muted">
                          {formatDateTime(log.createdAt)}
                        </span>
                      </div>
                      {log.reason && (
                        <div className="mba-action-log-reason">{log.reason}</div>
                      )}
                    </li>
                  ))}
                </ul>
              )}
            </>
          )}
        </div>
      </div>

      {pendingAction && (
        <div className="mba-confirm-overlay" onClick={(e) => e.stopPropagation()}>
          <div className="mba-confirm-box">
            <div className="mba-confirm-title">{pendingAction.title}</div>
            <div className="mba-confirm-desc">{pendingAction.desc}</div>
            <textarea
              className="mba-confirm-textarea"
              placeholder={
                pendingAction.requireReason ? "사유 (필수)" : "사유 (선택)"
              }
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
            {actionError && <div className="mba-alert">{actionError}</div>}
            <div className="mba-confirm-actions">
              <button
                type="button"
                className="mba-btn"
                onClick={closeConfirm}
                disabled={submitting}
              >
                취소
              </button>
              <button
                type="button"
                className="mba-btn mba-btn-danger"
                onClick={submitAction}
                disabled={submitting}
              >
                {submitting ? "처리 중..." : pendingAction.confirmLabel}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default MemberDetailDrawer;
