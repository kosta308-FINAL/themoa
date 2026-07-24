import { useState } from "react";
import {
  createManualTransaction,
  updateManualTransaction,
} from "../../api/spendingGuideApi";
import DashboardIcon from "../../components/common/DashboardIcon";
import SelectFieldModal from "./components/SelectFieldModal";
import DateTimeFieldModal from "./components/DateTimeFieldModal";

const WON = new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 0 });
const nowValue = () => {
  const now = new Date();
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  return now.toISOString().slice(0, 16);
};

function ManualTransactionModal({
  categories,
  transaction = null,
  allowCard = false,
  onClose,
  onSaved,
}) {
  const [form, setForm] = useState({
    amount: transaction ? String(Number(transaction.amount)) : "",
    paymentMethod: transaction?.paymentMethod || "",
    merchantName:
      transaction?.merchantDisplayName || transaction?.merchantNameRaw || "",
    usedAt: transaction?.usedAt?.slice(0, 16) || nowValue(),
    categoryId: transaction?.categoryId ? String(transaction.categoryId) : "",
    memo: transaction?.memo || "",
  });
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const update = (key) => (event) =>
    setForm((current) => ({ ...current, [key]: event.target.value }));
  const setField = (key) => (value) =>
    setForm((current) => ({ ...current, [key]: value }));
  const handleAmount = (event) =>
    setForm((current) => ({
      ...current,
      amount: event.target.value.replace(/\D/g, "").slice(0, 12),
    }));

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!form.paymentMethod) {
      setError("결제수단을 선택해주세요.");
      return;
    }
    if (!form.categoryId) {
      setError("카테고리를 선택해주세요.");
      return;
    }
    setError("");
    setIsSubmitting(true);
    const [usedDate, usedTime] = form.usedAt.split("T");
    const payload = {
      paymentMethod: form.paymentMethod,
      usedDate,
      usedTime: usedTime ? `${usedTime}:00` : null,
      amount: Number(form.amount),
      categoryId: Number(form.categoryId),
      merchantName: form.merchantName.trim(),
      memo: form.memo.trim() || null,
    };
    try {
      if (transaction) await updateManualTransaction(transaction.id, payload);
      else await createManualTransaction(payload);
      await onSaved();
      onClose();
    } catch (requestError) {
      setError(
        requestError.response?.data?.message ||
          `지출을 ${transaction ? "수정" : "기록"}하지 못했습니다.`,
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const showCard = allowCard || transaction?.paymentMethod === "CARD";

  return (
    <div
      className="spending-modal-backdrop spending-manual-backdrop"
      role="presentation"
      onMouseDown={onClose}
    >
      <section
        className="spending-modal spending-manual-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="manual-entry-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="spending-modal-head">
          <h2 id="manual-entry-title">
            {transaction ? "지출 수정" : "지출 직접 입력"}
          </h2>
          <button
            type="button"
            className="spending-modal-close"
            onClick={onClose}
            aria-label="닫기"
          >
            <DashboardIcon name="x" />
          </button>
        </div>
        <form className="spending-entry-form" onSubmit={handleSubmit}>
          <label>
            <span>금액 *</span>
            <div className="spending-input-suffix">
              <input
                inputMode="numeric"
                value={form.amount ? WON.format(Number(form.amount)) : ""}
                onChange={handleAmount}
                placeholder="0"
                required
              />
              <em>원</em>
            </div>
          </label>
          <SelectFieldModal
            label="결제수단 *"
            value={form.paymentMethod}
            onChange={setField("paymentMethod")}
            options={[
              ...(showCard ? [{ value: "CARD", label: "카드" }] : []),
              { value: "CASH", label: "현금" },
              { value: "TRANSFER", label: "계좌이체" },
            ]}
          />
          <label className="wide">
            <span>사용처/내용 *</span>
            <input
              value={form.merchantName}
              onChange={update("merchantName")}
              maxLength={255}
              placeholder="예: 점심 식사, 친구에게 송금"
              required
            />
          </label>
          <DateTimeFieldModal
            label="사용일시 *"
            value={form.usedAt}
            onChange={setField("usedAt")}
            max={nowValue()}
          />
          <SelectFieldModal
            label="카테고리 *"
            value={form.categoryId}
            onChange={setField("categoryId")}
            disabled={!categories?.length}
            options={(categories || []).map((category) => ({
              value: String(category.id),
              label: category.name,
            }))}
          />
          <label className="wide">
            <span>메모</span>
            <textarea
              value={form.memo}
              onChange={update("memo")}
              maxLength={2000}
              placeholder="기억해둘 내용을 적어주세요"
            />
          </label>
          <div className="spending-form-notice wide">
            <span className="spending-form-notice-icon">
              <DashboardIcon name="info" size={15} />
            </span>
            <span>
              {allowCard
                ? "카드 자동수집이 꺼져 있어 카드 지출도 직접 입력할 수 있어요."
                : "카드 결제는 자동으로 불러오므로 직접 입력할 수 없어요."}{" "}
              매달 반복되는 지출은 고정지출 메뉴에서 등록해주세요.
            </span>
          </div>
          {error && (
            <div className="spending-form-error wide">
              <DashboardIcon name="info" size={16} />
              {error}
            </div>
          )}
          <button
            type="submit"
            className="spending-primary wide"
            disabled={isSubmitting || !categories?.length}
          >
            {isSubmitting
              ? "저장 중..."
              : transaction
                ? "지출 수정하기"
                : "지출 기록하기"}
          </button>
        </form>
      </section>
    </div>
  );
}

export default ManualTransactionModal;
