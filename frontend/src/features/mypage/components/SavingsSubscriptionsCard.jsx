import { useEffect, useState } from "react";
import {
  deleteSubscription,
  getSubscriptions,
  updateSubscriptionCondition,
} from "../../../api/savingsSubscriptionApi";
import BankBadge from "../../../components/common/BankBadge";
import "./SavingsSubscriptionsCard.css";

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
 * 마이페이지 "가입한 예·적금". 실제 가입한 상품을 등록해두고 만기 예상·우대조건 충족을 관리한다.
 * 우대조건 체크박스를 누르면 곧바로 서버에 반영(PATCH)하고 목록을 다시 받아 미충족 수를 갱신한다.
 */
function SavingsSubscriptionsCard() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);

  // 최초 로딩 + 다른 화면에서 가입 등록 후 돌아왔을 때 갱신(효과 본문에서 직접 setState 하지 않는다).
  useEffect(() => {
    let active = true;

    const fetchList = () => {
      getSubscriptions()
        .then((data) => {
          if (active) {
            setItems(data || []);
            setError("");
          }
        })
        .catch(() => {
          if (active) setError("가입 상품을 불러오지 못했습니다.");
        })
        .finally(() => {
          if (active) setLoading(false);
        });
    };

    const refreshIfVisible = () => {
      if (document.visibilityState === "visible") fetchList();
    };

    fetchList();
    window.addEventListener("focus", refreshIfVisible);
    document.addEventListener("visibilitychange", refreshIfVisible);

    return () => {
      active = false;
      window.removeEventListener("focus", refreshIfVisible);
      document.removeEventListener("visibilitychange", refreshIfVisible);
    };
  }, []);

  const reload = async () => {
    setItems((await getSubscriptions()) || []);
  };

  const reloadWithState = async () => {
    setLoading(true);
    setError("");
    try {
      await reload();
    } catch {
      setError("가입 상품을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const handleToggleCondition = async (condition) => {
    setBusyId(`cond:${condition.id}`);
    try {
      await updateSubscriptionCondition(condition.id, !condition.met);
      await reload();
    } catch {
      setError("우대조건 상태를 변경하지 못했습니다.");
    } finally {
      setBusyId(null);
    }
  };

  const handleDelete = async (item) => {
    setBusyId(`del:${item.id}`);
    try {
      await deleteSubscription(item.id);
      setItems((prev) => prev.filter((v) => v.id !== item.id));
    } catch {
      setError("삭제하지 못했습니다.");
    } finally {
      setBusyId(null);
    }
  };

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
          <button type="button" onClick={reloadWithState}>
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
        <div className="mp-sub-list">
          {items.map((item) => (
            <article className="mp-sub-item" key={item.id}>
              <div className="mp-sub-item-head">
                <div className="mp-sub-item-left">
                  <BankBadge companyName={item.companyName} size={38} />
                  <div className="mp-sub-item-title">
                    <span className="mp-sub-type">
                      {PRODUCT_TYPE_LABELS[item.productType] ||
                        item.productType}
                    </span>
                    <h3>{item.productName}</h3>
                    <p>{item.companyName}</p>
                  </div>
                </div>
                <button
                  type="button"
                  className="mp-sub-delete"
                  disabled={busyId === `del:${item.id}`}
                  onClick={() => handleDelete(item)}
                >
                  {busyId === `del:${item.id}` ? "처리 중..." : "삭제"}
                </button>
              </div>

              <div className="mp-sub-meta">
                <span>월 {won(item.monthlyAmount)}</span>
                <span>{item.appliedRate}%</span>
                <span>{item.termMonth}개월</span>
              </div>

              <div className="mp-sub-maturity">
                <div>
                  <span>만기 예상</span>
                  <strong>{won(item.expectedMaturityAmount)}</strong>
                </div>
                <div>
                  <span>원금 합계</span>
                  <b>{won(item.totalPrincipal)}</b>
                </div>
                {item.maturityDate && (
                  <div>
                    <span>만기일</span>
                    <b>{item.maturityDate}</b>
                  </div>
                )}
              </div>

              {item.unmetConditionCount > 0 && (
                <p className="mp-sub-warn">
                  ⚠️ 우대조건 {item.unmetConditionCount}개 미충족
                </p>
              )}

              {item.conditions?.length > 0 && (
                <ul className="mp-sub-conditions">
                  {item.conditions.map((condition) => (
                    <li key={condition.id}>
                      <label>
                        <input
                          type="checkbox"
                          checked={condition.met}
                          disabled={busyId === `cond:${condition.id}`}
                          onChange={() => handleToggleCondition(condition)}
                        />
                        <span>{condition.description}</span>
                      </label>
                      <em>+{condition.rateBonus}%p</em>
                    </li>
                  ))}
                </ul>
              )}
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

export default SavingsSubscriptionsCard;
