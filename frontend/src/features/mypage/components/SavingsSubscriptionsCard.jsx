import { useState } from "react";
import SubscriptionItem from "../../../components/common/SubscriptionItem";
import SubscriptionEditModal from "../../../components/common/SubscriptionEditModal";
import { useSavingsSubscriptions } from "../../../hooks/useSavingsSubscriptions";
import "./SavingsSubscriptionsCard.css";

const won = (value) =>
  value == null ? "-" : `${Number(value).toLocaleString("ko-KR")}원`;

const sumBy = (items, pick) =>
  items.reduce((sum, item) => sum + (Number(pick(item)) || 0), 0);

/**
 * 마이페이지 "가입한 예·적금". 실제 가입한 상품을 등록해두고 만기 예상·우대조건 충족을 관리한다.
 * 상품명/수정을 누르면 편집 모달이 열리고, 저장하면 목록을 다시 받아 파생값(현재/만기)을 갱신한다.
 */
function SavingsSubscriptionsCard() {
  const subscriptions = useSavingsSubscriptions();
  const { items, loading, error, busyId } = subscriptions;
  const [editItem, setEditItem] = useState(null);

  const totalCurrent = sumBy(items, (item) => item.currentValue);
  const totalMaturity = sumBy(items, (item) => item.expectedMaturityAmount);

  return (
    <section className="mp-card mp-sub-card">
      <div className="mp-card-head">
        <div>
          <h2>가입한 예·적금</h2>
          <p className="mp-card-sub">총 {items.length}개</p>
        </div>
      </div>

      {loading && <p className="mp-empty">가입 상품을 불러오고 있어요.</p>}

      {!loading && error && (
        <div className="mp-sub-error">
          <span>{error}</span>
          <button type="button" onClick={subscriptions.reloadWithState}>
            다시 시도
          </button>
        </div>
      )}

      {!loading && !error && items.length === 0 && (
        <div className="mp-sub-empty">
          <p>아직 등록한 가입 상품이 없어요.</p>
          <span>추천·검색·관심 상품에서 "가입 등록"을 눌러 추가해 보세요.</span>
        </div>
      )}

      {!loading && !error && items.length > 0 && (
        <>
          <div className="mp-sub-totals">
            <div className="mp-sub-total mp-sub-total-current">
              <span>현재 평가 합계</span>
              <strong>{won(totalCurrent)}</strong>
            </div>
            <div className="mp-sub-total mp-sub-total-maturity">
              <span>만기 예상 합계</span>
              <strong>{won(totalMaturity)}</strong>
            </div>
          </div>
          <div className="mp-sub-list">
            {items.map((item) => (
              <SubscriptionItem
                key={item.id}
                item={item}
                busyId={busyId}
                onEdit={setEditItem}
                onDelete={subscriptions.remove}
                onToggleCondition={subscriptions.toggleCondition}
              />
            ))}
          </div>
        </>
      )}

      {editItem && (
        <SubscriptionEditModal
          subscription={editItem}
          onClose={() => setEditItem(null)}
          onSaved={() => subscriptions.reload()}
        />
      )}
    </section>
  );
}

export default SavingsSubscriptionsCard;
