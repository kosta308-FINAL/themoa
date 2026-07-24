import { useState } from "react";
import DashboardIcon from "../../../components/common/DashboardIcon";
import { dismissFixedExpenseCoachingCard } from "../../../api/fixedExpenseApi";

/** 고정지출 "연 환산" 코칭 카드. 절감을 종용하지 않고 담담한 정보만 보여준다 — 문구는 서버가 완성해 내려준다. */
function FixedExpenseCoachingCards({ cards, onDismissed }) {
  const [dismissingId, setDismissingId] = useState(null);

  if (!cards?.length) return null;

  const handleDismiss = async (cardId) => {
    setDismissingId(cardId);
    try {
      await dismissFixedExpenseCoachingCard(cardId);
      await onDismissed();
    } catch {
      // 넘기기 실패는 다음 새로고침에서 다시 시도할 수 있게 조용히 넘어간다.
    } finally {
      setDismissingId(null);
    }
  };

  return (
    <section
      className="fx-panel fx-coaching-panel"
      aria-label="고정지출 연 환산 안내"
    >
      <div className="fx-panel-head">
        <div className="fx-panel-title">
          <span className="fx-panel-title-icon fx-tone-purple">
            <DashboardIcon name="chart" size={18} />
          </span>
          <div>
            <h2>연으로 보면 이만큼이에요</h2>
            <p>매달은 작아 보여도 1년으로 보면 이 정도예요.</p>
          </div>
        </div>
      </div>
      <div className="fx-coaching-list">
        {cards.map((card) => (
          <article className="fx-coaching-card" key={card.id}>
            <div className="fx-coaching-body">
              <strong>{card.title}</strong>
              <p>{card.body}</p>
            </div>
            <button
              type="button"
              className="fx-ghost-button"
              disabled={dismissingId === card.id}
              onClick={() => handleDismiss(card.id)}
            >
              다시 보지 않기
            </button>
          </article>
        ))}
      </div>
    </section>
  );
}

export default FixedExpenseCoachingCards;
