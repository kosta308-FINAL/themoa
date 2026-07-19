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
}) {
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
        {candidates.map((candidate) => (
          <article className="fx-suggestion-card" key={candidate.id}>
            <div className="fx-suggestion-top">
              <span
                className={`fx-service-icon ${toneForId(candidate.recurringGroupId)}`}
              >
                {serviceInitial(candidate.merchantAliasName)}
              </span>
              <div className="fx-service-info">
                <strong>{candidate.merchantAliasName}</strong>
                <span>
                  매월 {candidate.avgPayDay}일쯤 · {formatWon(candidate.avgAmount)}
                </span>
              </div>
            </div>
            <p className="fx-suggestion-message">
              {candidate.recommendMessage ||
                "최근 몇 달 동안 비슷한 날짜에 같은 금액이 결제됐어요. 고정지출로 등록할까요?"}
            </p>
            <div className="fx-suggestion-actions">
              <button
                type="button"
                className="fx-primary-button"
                disabled={pendingId === candidate.id}
                onClick={() => onRegister(candidate)}
              >
                등록
              </button>
              <button
                type="button"
                className="fx-ghost-button"
                disabled={pendingId === candidate.id}
                onClick={() => onSnooze(candidate.id)}
              >
                나중에
              </button>
              <button
                type="button"
                className="fx-more-button"
                disabled={pendingId === candidate.id}
                aria-label="이 추천 거절"
                onClick={() => onReject(candidate.id)}
              >
                <MoreDotsIcon />
              </button>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

export default FixedExpenseSuggestions;
