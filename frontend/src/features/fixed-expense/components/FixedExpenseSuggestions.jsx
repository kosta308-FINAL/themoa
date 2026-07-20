import { useEffect, useRef, useState } from "react";
import DashboardIcon from "../../../components/common/DashboardIcon";
import { formatWon, serviceInitial, toneForId } from "../fixedExpenseUtils";

function MoreDotsIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
      <circle cx="5" cy="12" r="1.6" />
      <circle cx="12" cy="12" r="1.6" />
      <circle cx="19" cy="12" r="1.6" />
    </svg>
  );
}

function FixedExpenseSuggestions({
  candidates,
  pendingId,
  onRegister,
  onSnooze,
  onReject,
  onReclassifyHabit,
}) {
  const [openMenuId, setOpenMenuId] = useState(null);
  const menuRef = useRef(null);

  useEffect(() => {
    if (!openMenuId) return undefined;
    const closeIfOutside = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setOpenMenuId(null);
      }
    };
    document.addEventListener("mousedown", closeIfOutside);
    return () => document.removeEventListener("mousedown", closeIfOutside);
  }, [openMenuId]);

  if (!candidates?.length) return null;

  return (
    <section className="fx-panel" aria-label="새로 발견한 고정지출">
      <div className="fx-panel-head">
        <div className="fx-panel-title">
          <span className="fx-panel-title-icon fx-tone-orange">
            <DashboardIcon name="sparkle" size={18} />
          </span>
          <div>
            <h2>새로 발견한 고정지출</h2>
            <p>카드내역에서 매달 반복되는 결제를 찾았어요.</p>
          </div>
        </div>
        <span className="fx-panel-count">{candidates.length}건</span>
      </div>
      <div className="fx-suggestion-list">
        {candidates.map((candidate) => {
          const isPending = pendingId === candidate.id;
          const isMenuOpen = openMenuId === candidate.id;
          return (
            <article
              className={`fx-suggestion-card${isPending ? " is-pending" : ""}`}
              key={candidate.id}
            >
              <div className="fx-suggestion-top">
                <span
                  className={`fx-service-icon ${toneForId(candidate.recurringGroupId)}`}
                >
                  {serviceInitial(candidate.merchantAliasName)}
                </span>
                <div className="fx-service-info">
                  <strong>{candidate.merchantAliasName}</strong>
                  <span>
                    매월 {candidate.avgPayDay}일쯤 ·{" "}
                    {formatWon(candidate.avgAmount)}
                  </span>
                </div>
                <button
                  type="button"
                  className="fx-suggestion-register"
                  disabled={isPending}
                  onClick={() => onRegister(candidate)}
                >
                  등록
                </button>
              </div>

              <div className="fx-suggestion-actions">
                <button
                  type="button"
                  className="fx-suggestion-action"
                  disabled={isPending}
                  onClick={() => onSnooze(candidate.id)}
                >
                  나중에
                </button>
                <button
                  type="button"
                  className="fx-suggestion-action"
                  disabled={isPending}
                  onClick={() => onReclassifyHabit(candidate.id)}
                >
                  습관적 소비로 분류
                </button>
                <div
                  className="fx-more-menu-wrap"
                  ref={isMenuOpen ? menuRef : undefined}
                >
                  <button
                    type="button"
                    className="fx-more-button"
                    disabled={isPending}
                    aria-label="추가 옵션"
                    aria-expanded={isMenuOpen}
                    onClick={() =>
                      setOpenMenuId(isMenuOpen ? null : candidate.id)
                    }
                  >
                    <MoreDotsIcon />
                  </button>
                  {isMenuOpen && (
                    <div className="fx-more-menu" role="menu">
                      <button
                        type="button"
                        className="fx-more-menu-item danger"
                        role="menuitem"
                        onClick={() => {
                          setOpenMenuId(null);
                          onReject(candidate.id);
                        }}
                      >
                        이 추천 거절하기
                      </button>
                    </div>
                  )}
                </div>
              </div>
            </article>
          );
        })}
      </div>
    </section>
  );
}

export default FixedExpenseSuggestions;
