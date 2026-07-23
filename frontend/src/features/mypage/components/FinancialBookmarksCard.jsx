import { useEffect, useState } from "react";
import { getBookmarks, removeBookmark } from "../../../api/bookmarkApi";
import BankBadge from "../../../components/common/BankBadge";
import SavingsSubscriptionModal from "../../../components/common/SavingsSubscriptionModal";
import "./FinancialBookmarksCard.css";

// 상품 종류 배지. 대출 3종은 묶어서 "대출"로 표기한다.
const PRODUCT_TYPE_LABELS = {
  DEPOSIT: "정기예금",
  SAVING: "적금",
  MORTGAGE: "대출",
  RENT: "대출",
  CREDIT: "대출",
};

/**
 * 마이페이지 "관심 상품". 북마크한 상품을 검색 결과 카드처럼 상세히 보여준다.
 * (종류 배지·가입방법·우대조건·홈페이지 링크·판매종료 여부, 예·적금은 "가입 등록"까지)
 */
function FinancialBookmarksCard() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);
  const [registerProductId, setRegisterProductId] = useState(null);
  // 우대조건 원문이 길어 항목별로 접고 펼친다(펼친 bookmarkId 집합).
  const [openConditionIds, setOpenConditionIds] = useState([]);

  // 최초 진입 로딩 + 화면이 다시 활성화될 때 갱신.
  // 다른 탭/창에서 북마크를 바꾸고 돌아오면 이 화면은 이미 떠 있어서 다시 불러오지 않는다 —
  // 그래서 focus·visibilitychange 시점에도 목록을 새로 받아 새로고침 없이 최신 상태를 보여준다.
  // (효과 본문에서 곧바로 setState 하지 않도록 상태 변경은 응답 콜백에서만 한다)
  useEffect(() => {
    let active = true;

    const fetchBookmarks = () => {
      getBookmarks()
        .then((data) => {
          if (active) {
            setItems(data || []);
            setError("");
          }
        })
        .catch(() => {
          if (active) {
            setError("관심 상품을 불러오지 못했습니다.");
          }
        })
        .finally(() => {
          if (active) {
            setLoading(false);
          }
        });
    };

    const refreshIfVisible = () => {
      if (document.visibilityState === "visible") {
        fetchBookmarks();
      }
    };

    fetchBookmarks();
    window.addEventListener("focus", refreshIfVisible);
    document.addEventListener("visibilitychange", refreshIfVisible);

    return () => {
      active = false;
      window.removeEventListener("focus", refreshIfVisible);
      document.removeEventListener("visibilitychange", refreshIfVisible);
    };
  }, []);

  // "다시 시도" 버튼용. 이벤트 핸들러라 즉시 로딩 상태로 바꿔도 된다.
  const reloadBookmarks = async () => {
    setLoading(true);
    setError("");
    try {
      setItems((await getBookmarks()) || []);
    } catch {
      setError("관심 상품을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const handleRemove = async (item) => {
    setBusyId(item.bookmarkId);
    try {
      await removeBookmark({
        targetType: item.targetType,
        targetId: item.targetId,
      });
      setItems((prev) => prev.filter((v) => v.bookmarkId !== item.bookmarkId));
    } catch {
      setError("북마크 해제에 실패했습니다.");
    } finally {
      setBusyId(null);
    }
  };

  const toggleCondition = (bookmarkId) => {
    setOpenConditionIds((prev) =>
      prev.includes(bookmarkId)
        ? prev.filter((id) => id !== bookmarkId)
        : [...prev, bookmarkId],
    );
  };

  return (
    <section className="mp-card mp-financial-bookmark-card">
      <div className="mp-card-head">
        <div>
          <h2>관심 상품</h2>
          <p className="mp-card-sub">총 {items.length}개</p>
        </div>
      </div>

      {loading && <p className="mp-empty">관심 상품을 불러오고 있어요.</p>}

      {!loading && error && (
        <div className="mp-financial-bookmark-error">
          <span>{error}</span>
          <button type="button" onClick={reloadBookmarks}>
            다시 시도
          </button>
        </div>
      )}

      {!loading && !error && items.length === 0 && (
        <div className="mp-financial-bookmark-empty">
          <p>아직 저장한 상품이 없어요.</p>
          <span>금융상품 추천·검색에서 북마크를 눌러 보세요.</span>
        </div>
      )}

      {!loading && !error && items.length > 0 && (
        <div className="mp-financial-bookmark-list">
          {items.map((item) => {
            const conditionOpen = openConditionIds.includes(item.bookmarkId);
            return (
              <article
                className="mp-financial-bookmark-item"
                key={item.bookmarkId}
              >
                <div className="mp-financial-bookmark-badges">
                  {item.discontinued && (
                    <span className="mp-financial-bookmark-discontinued">
                      판매종료
                    </span>
                  )}
                  <span className="mp-financial-bookmark-type">
                    {PRODUCT_TYPE_LABELS[item.productType] || item.productType}
                  </span>
                  {item.rate != null && (
                    <span className="mp-financial-bookmark-rate">
                      {item.rate}%
                      {item.termMonth != null && ` · ${item.termMonth}개월`}
                    </span>
                  )}
                </div>

                <div className="mp-financial-bookmark-titlerow">
                  <BankBadge companyName={item.subtitle} size={36} />
                  <div>
                    <h3>{item.title}</h3>
                    <p className="mp-financial-bookmark-sub">{item.subtitle}</p>
                  </div>
                </div>

                {item.joinMethod && (
                  <p className="mp-financial-bookmark-join">
                    가입방법 · {item.joinMethod}
                  </p>
                )}

                {item.specialCondition && (
                  <div className="mp-financial-bookmark-special">
                    <button
                      type="button"
                      onClick={() => toggleCondition(item.bookmarkId)}
                    >
                      {conditionOpen ? "▾ 우대조건 접기" : "▸ 우대조건 보기"}
                    </button>
                    {conditionOpen && <p>{item.specialCondition}</p>}
                  </div>
                )}

                <div className="mp-financial-bookmark-actions">
                  {item.officialUrl && (
                    <a
                      href={item.officialUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="mp-financial-bookmark-link"
                    >
                      🔗 홈페이지 바로가기
                    </a>
                  )}
                  {item.targetType === "SAVINGS_PRODUCT" && (
                    <button
                      type="button"
                      className="mp-financial-bookmark-register"
                      onClick={() => setRegisterProductId(item.targetId)}
                    >
                      가입 등록
                    </button>
                  )}
                  <button
                    type="button"
                    className="mp-financial-bookmark-remove"
                    disabled={busyId === item.bookmarkId}
                    onClick={() => handleRemove(item)}
                  >
                    {busyId === item.bookmarkId ? "처리 중..." : "북마크 해제"}
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      )}

      {registerProductId != null && (
        <SavingsSubscriptionModal
          productId={registerProductId}
          onClose={() => setRegisterProductId(null)}
        />
      )}
    </section>
  );
}

export default FinancialBookmarksCard;
