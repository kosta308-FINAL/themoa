import BankBadge from "./BankBadge";
import "./SubscriptionItem.css";

const PRODUCT_TYPE_LABELS = {
  DEPOSIT: "정기예금",
  SAVING: "적금",
  MORTGAGE: "주택담보대출",
  RENT: "전세자금대출",
  CREDIT: "개인신용대출",
};

const won = (value) =>
  value == null ? "-" : `${Number(value).toLocaleString("ko-KR")}원`;

/**
 * 가입한 예·적금 항목 1건(마이페이지 카드·관리 페이지 공용).
 * 상품명을 누르거나 "수정"을 누르면 onEdit로 편집을 연다. 원금/현재/만기 세 금액을 색으로 구분한다.
 */
function SubscriptionItem({
  item,
  busyId,
  onEdit,
  onDelete,
  onToggleCondition,
}) {
  return (
    <article className="si-item">
      <div className="si-head">
        <div className="si-left">
          <BankBadge companyName={item.companyName} size={38} />
          <div className="si-title">
            <span className="si-type">
              {PRODUCT_TYPE_LABELS[item.productType] || item.productType}
            </span>
            <button
              type="button"
              className="si-name"
              onClick={() => onEdit(item)}
            >
              {item.productName}
            </button>
            <p>{item.companyName}</p>
          </div>
        </div>
        <div className="si-head-actions">
          <button
            type="button"
            className="si-edit"
            onClick={() => onEdit(item)}
          >
            수정
          </button>
          <button
            type="button"
            className="si-delete"
            disabled={busyId === `del:${item.id}`}
            onClick={() => onDelete(item)}
          >
            {busyId === `del:${item.id}` ? "처리 중..." : "삭제"}
          </button>
        </div>
      </div>

      <div className="si-meta">
        <span>월 {won(item.monthlyAmount)}</span>
        <span>{item.appliedRate}%</span>
        <span>{item.termMonth}개월</span>
      </div>

      <div className="si-amounts">
        <div className="si-amount si-principal">
          <span>납입 원금</span>
          <strong>{won(item.currentPrincipal)}</strong>
        </div>
        <div className="si-amount si-current">
          <span>현재 평가</span>
          <strong>{won(item.currentValue)}</strong>
        </div>
        <div className="si-amount si-maturity">
          <span>만기 예상</span>
          <strong>{won(item.expectedMaturityAmount)}</strong>
        </div>
      </div>
      <p className="si-amounts-note">
        현재 평가는 경과이자를 더한 세전 추정치예요.
        {item.maturityDate && ` · 만기일 ${item.maturityDate}`}
      </p>

      {item.unmetConditionCount > 0 && (
        <p className="si-warn">
          ⚠️ 우대조건 {item.unmetConditionCount}개 미충족
        </p>
      )}

      {item.conditions?.length > 0 && (
        <ul className="si-conditions">
          {item.conditions.map((condition) => (
            <li key={condition.id}>
              <label>
                <input
                  type="checkbox"
                  checked={condition.met}
                  disabled={busyId === `cond:${condition.id}`}
                  onChange={() => onToggleCondition(condition)}
                />
                <span>{condition.description}</span>
              </label>
              <em>+{condition.rateBonus}%p</em>
            </li>
          ))}
        </ul>
      )}
    </article>
  );
}

export default SubscriptionItem;
