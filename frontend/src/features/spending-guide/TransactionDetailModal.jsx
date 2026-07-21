import { useCallback, useEffect, useState } from "react";
import {
  correctTransactionAmount,
  correctTransactionCanceledAmount,
  deleteManualTransaction,
  getTransactionDetail,
  updateTransactionCategory,
  updateTransactionMemo,
} from "../../api/spendingGuideApi";
import DashboardIcon from "../../components/common/DashboardIcon";

const WON = new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 0 });
const toNumber = (value) => Number(value ?? 0);
const formatWon = (value) => `${WON.format(Math.abs(toNumber(value)))}원`;
const digits = (value) => value.replace(/\D/g, "").slice(0, 12);
const formatForeign = (value, currencyCode) => {
  if (value == null) return "—";
  const amount = Number(value).toLocaleString("en-US", {
    maximumFractionDigits: 2,
  });
  return [amount, currencyCode].filter(Boolean).join(" ");
};
const signedAmount = (value) => {
  const amount = toNumber(value);
  return `${amount > 0 ? "-" : amount < 0 ? "+" : ""}${formatWon(amount)}`;
};
const formatDateTime = (value) => {
  const matched = String(value || "").match(
    /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/,
  );
  if (!matched) return "—";
  return `${matched[1]}. ${Number(matched[2])}. ${Number(matched[3])}. ${matched[4]}:${matched[5]}`;
};
const dayOfMonth = (value) => {
  const matched = String(value || "").match(/^\d{4}-\d{2}-(\d{2})/);
  return matched ? String(Number(matched[1])) : "";
};
const paymentLabel = (transaction) => {
  if (transaction.paymentMethod === "CASH") return "현금";
  if (transaction.paymentMethod === "TRANSFER") return "계좌이체";
  const cardSuffix = transaction.cardNumberMasked
    ? String(transaction.cardNumberMasked).slice(-4)
    : "";
  return (
    [transaction.cardOrganizationName, cardSuffix]
      .filter(Boolean)
      .join(" · ") || "카드"
  );
};
const transactionIcon = (transaction) => {
  const category = transaction.categoryName || "";
  if (/카페|간식/.test(category)) return "coffee";
  if (/식비|배달|외식/.test(category)) return "utensils";
  if (/교통|택시|주유|차량/.test(category)) return "car";
  if (/편의점|마트|쇼핑/.test(category)) return "bag";
  return transaction.paymentMethod === "CARD" ? "card" : "receipt";
};

function TransactionDetailModal({
  transactionId,
  categories,
  onClose,
  onChanged,
  onRegisterFixedExpense,
}) {
  const [transaction, setTransaction] = useState(null);
  const [categoryId, setCategoryId] = useState("");
  const [memo, setMemo] = useState("");
  const [categoryOpen, setCategoryOpen] = useState(false);
  const [memoOpen, setMemoOpen] = useState(false);
  const [canceledAmountOpen, setCanceledAmountOpen] = useState(false);
  const [canceledAmountInput, setCanceledAmountInput] = useState("");
  const [amountOpen, setAmountOpen] = useState(false);
  const [amountInput, setAmountInput] = useState("");
  const [error, setError] = useState("");
  const [pending, setPending] = useState("");

  const applyResponse = (response) => {
    setTransaction(response);
    setCategoryId(String(response.categoryId || ""));
    setMemo(response.memo || "");
    setCanceledAmountInput(
      response.canceledAmount != null
        ? String(Number(response.canceledAmount))
        : "",
    );
    setAmountInput(
      response.amount != null ? String(Number(response.amount)) : "",
    );
  };

  const load = useCallback(async () => {
    const response = await getTransactionDetail(transactionId);
    applyResponse(response);
  }, [transactionId]);

  useEffect(() => {
    let active = true;
    getTransactionDetail(transactionId)
      .then((response) => {
        if (!active) return;
        applyResponse(response);
      })
      .catch((requestError) => {
        if (active)
          setError(
            requestError.response?.data?.message ||
              "거래 상세를 불러오지 못했습니다.",
          );
      });
    return () => {
      active = false;
    };
  }, [transactionId]);

  const save = async (type, request) => {
    setError("");
    setPending(type);
    try {
      await request();
      await load();
      await onChanged();
      return true;
    } catch (requestError) {
      setError(
        requestError.response?.data?.message ||
          "변경사항을 저장하지 못했습니다.",
      );
      return false;
    } finally {
      setPending("");
    }
  };

  const handleCategoryChange = async (category) => {
    const saved = await save("category", () =>
      updateTransactionCategory(transactionId, Number(category.id)),
    );
    if (saved) setCategoryOpen(false);
  };

  const handleMemoSave = async () => {
    const saved = await save("memo", () =>
      updateTransactionMemo(transactionId, memo.trim() || null),
    );
    if (saved) setMemoOpen(false);
  };

  const handleCanceledAmountSave = async () => {
    const saved = await save("canceledAmount", () =>
      correctTransactionCanceledAmount(
        transactionId,
        Number(canceledAmountInput || 0),
      ),
    );
    if (saved) setCanceledAmountOpen(false);
  };

  const handleAmountSave = async () => {
    const saved = await save("amount", () =>
      correctTransactionAmount(transactionId, Number(amountInput || 0)),
    );
    if (saved) setAmountOpen(false);
  };

  const handleRegisterFixedExpense = () => {
    if (!transaction || !onRegisterFixedExpense) return;
    const name =
      transaction.merchantDisplayName || transaction.merchantNameRaw || "";
    onRegisterFixedExpense({
      transactionId,
      name,
      method: transaction.paymentMethod === "TRANSFER" ? "TRANSFER" : "CARD",
      merchantAliasId: transaction.merchantAliasId ?? null,
      merchantName: name,
      categoryId: transaction.categoryId ? String(transaction.categoryId) : "",
      amount:
        transaction.netAmount != null
          ? String(Math.round(Number(transaction.netAmount)))
          : "",
      payDay: dayOfMonth(transaction.usedAt),
    });
    onClose();
  };

  const handleDelete = async () => {
    if (
      !window.confirm("이 지출 내역을 삭제할까요? 삭제하면 되돌릴 수 없습니다.")
    )
      return;
    setError("");
    setPending("delete");
    try {
      await deleteManualTransaction(transactionId);
      await onChanged();
      onClose();
    } catch (requestError) {
      setError(
        requestError.response?.data?.message ||
          "지출 내역을 삭제하지 못했습니다.",
      );
      setPending("");
    }
  };

  return (
    <div
      className="spending-modal-backdrop spending-detail-backdrop"
      role="presentation"
      onMouseDown={onClose}
    >
      <section
        className="spending-modal spending-detail-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="transaction-detail-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="spending-modal-head">
          <h2 id="transaction-detail-title">거래 상세</h2>
          <button
            type="button"
            className="spending-modal-close"
            onClick={onClose}
            aria-label="닫기"
          >
            <DashboardIcon name="x" />
          </button>
        </div>
        {error && (
          <div className="spending-section-error">
            <DashboardIcon name="info" size={18} />
            {error}
          </div>
        )}
        {!error && !transaction && (
          <div className="spending-loading">
            <span className="spending-spinner" />
            거래 상세를 불러오는 중...
          </div>
        )}
        {transaction && (
          <div className="spending-detail-body">
            <div className="spending-detail-hero">
              <span className="spending-detail-transaction-icon">
                <DashboardIcon name={transactionIcon(transaction)} />
              </span>
              <strong>{signedAmount(transaction.netAmount)}</strong>
              <span>
                {transaction.merchantDisplayName || transaction.merchantNameRaw}
              </span>
            </div>
            <div className="spending-detail-list">
              <div className="spending-detail-row">
                <span>사용 일시</span>
                <strong>{formatDateTime(transaction.usedAt)}</strong>
              </div>
              <div className="spending-detail-row">
                <span>결제수단</span>
                <strong>{paymentLabel(transaction)}</strong>
              </div>
              <div className="spending-detail-row">
                <span>입력 구분</span>
                <strong>
                  {transaction.source === "MANUAL"
                    ? "직접 입력"
                    : "카드 자동수집"}
                </strong>
              </div>
              <div className="spending-detail-row">
                <span>카테고리</span>
                <strong>
                  {transaction.categoryName ||
                    categories?.find(
                      (category) => String(category.id) === categoryId,
                    )?.name ||
                    "미분류"}
                </strong>
                <button
                  type="button"
                  onClick={() => setCategoryOpen((open) => !open)}
                >
                  변경
                </button>
              </div>
              <div
                className={`spending-detail-category-picker${categoryOpen ? " open" : ""}`}
              >
                {(categories || []).map((category) => (
                  <button
                    type="button"
                    key={category.id}
                    disabled={Boolean(pending)}
                    onClick={() => handleCategoryChange(category)}
                  >
                    {category.name}
                  </button>
                ))}
              </div>
              <div className="spending-detail-row">
                <span>메모</span>
                <strong>{transaction.memo || "메모 없음"}</strong>
                <button
                  type="button"
                  onClick={() => setMemoOpen((open) => !open)}
                >
                  {transaction.memo ? "수정" : "추가"}
                </button>
              </div>
              <div
                className={`spending-detail-inline-edit${memoOpen ? " open" : ""}`}
              >
                <input
                  value={memo}
                  maxLength={100}
                  onChange={(event) => setMemo(event.target.value)}
                  placeholder="메모를 입력하세요"
                />
                <button
                  type="button"
                  className="spending-secondary"
                  disabled={Boolean(pending)}
                  onClick={handleMemoSave}
                >
                  저장
                </button>
              </div>
              {transaction.cancelAmountUncertain && (
                <>
                  <div className="spending-detail-row">
                    <span>취소금액</span>
                    <strong>
                      {transaction.canceledAmount != null
                        ? formatWon(transaction.canceledAmount)
                        : "확인 중"}
                    </strong>
                    <button
                      type="button"
                      onClick={() => setCanceledAmountOpen((open) => !open)}
                    >
                      정정
                    </button>
                  </div>
                  <div
                    className={`spending-detail-inline-edit${canceledAmountOpen ? " open" : ""}`}
                  >
                    <input
                      inputMode="numeric"
                      value={
                        canceledAmountInput
                          ? WON.format(Number(canceledAmountInput))
                          : ""
                      }
                      onChange={(event) =>
                        setCanceledAmountInput(digits(event.target.value))
                      }
                      placeholder="카드 명세서의 실제 취소금액을 입력하세요"
                    />
                    <button
                      type="button"
                      className="spending-secondary"
                      disabled={Boolean(pending) || canceledAmountInput === ""}
                      onClick={handleCanceledAmountSave}
                    >
                      저장
                    </button>
                  </div>
                </>
              )}
              {transaction.originalAmount != null && (
                <>
                  <div className="spending-detail-row">
                    <span>결제 원금</span>
                    <strong>
                      {formatForeign(
                        transaction.originalAmount,
                        transaction.currencyCode,
                      )}
                    </strong>
                  </div>
                  <div className="spending-detail-row">
                    <span>환산금액</span>
                    <strong>
                      {formatWon(transaction.amount)}
                      {transaction.amountUserCorrected ? "" : " (추정)"}
                    </strong>
                    <button
                      type="button"
                      onClick={() => setAmountOpen((open) => !open)}
                    >
                      정정
                    </button>
                  </div>
                  <div
                    className={`spending-detail-inline-edit${amountOpen ? " open" : ""}`}
                  >
                    <input
                      inputMode="numeric"
                      value={amountInput ? WON.format(Number(amountInput)) : ""}
                      onChange={(event) =>
                        setAmountInput(digits(event.target.value))
                      }
                      placeholder="카드 명세서의 실제 청구 원화를 입력하세요"
                    />
                    <button
                      type="button"
                      className="spending-secondary"
                      disabled={Boolean(pending) || !amountInput}
                      onClick={handleAmountSave}
                    >
                      저장
                    </button>
                  </div>
                </>
              )}
            </div>
            {onRegisterFixedExpense &&
              (transaction.paymentMethod === "CARD" ||
                transaction.paymentMethod === "TRANSFER") && (
                <button
                  type="button"
                  className="spending-detail-register-fixed"
                  onClick={handleRegisterFixedExpense}
                >
                  <DashboardIcon name="repeat" size={15} />이 거래로 고정지출
                  등록
                </button>
              )}
            {transaction.source === "MANUAL" && (
              <button
                type="button"
                className="spending-detail-delete"
                disabled={Boolean(pending)}
                onClick={handleDelete}
              >
                {pending === "delete" ? "삭제 중..." : "이 지출 내역 삭제"}
              </button>
            )}
          </div>
        )}
      </section>
    </div>
  );
}

export default TransactionDetailModal;
