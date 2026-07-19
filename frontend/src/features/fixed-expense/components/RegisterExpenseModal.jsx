import { useState } from "react";
import DashboardIcon from "../../../components/common/DashboardIcon";
import { getApiErrorMessage } from "../../../utils/apiError";
import {
  registerFixedExpenseDirect,
  registerFixedExpenseFromCandidate,
} from "../../../api/fixedExpenseApi";

const PAY_DAYS = Array.from({ length: 31 }, (_, index) => index + 1);
const WON = new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 0 });

function RegisterExpenseModal({ candidate = null, categories, onClose, onSaved }) {
  const [form, setForm] = useState({
    name: candidate?.merchantAliasName || "",
    method: "CARD",
    merchantName: candidate?.merchantAliasName || "",
    categoryId: candidate?.recommendedCategoryId
      ? String(candidate.recommendedCategoryId)
      : "",
    payDay: candidate?.avgPayDay ? String(candidate.avgPayDay) : "",
    amount: candidate?.avgAmount ? String(Math.round(Number(candidate.avgAmount))) : "",
    currency: "KRW",
  });
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const update = (key) => (event) =>
    setForm((current) => ({ ...current, [key]: event.target.value }));

  const handleAmount = (event) =>
    setForm((current) => ({
      ...current,
      amount: event.target.value.replace(/[^\d.]/g, ""),
    }));

  const isCard = form.method === "CARD";

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);
    try {
      if (candidate) {
        await registerFixedExpenseFromCandidate(candidate.id, {
          name: form.name.trim(),
          categoryId: form.categoryId ? Number(form.categoryId) : null,
          merchantAliasId: null,
          newMerchantAliasName: (form.merchantName || form.name).trim(),
          expectedAmount: Number(form.amount),
          expectedCurrency: form.currency,
          expectedPayDay: Number(form.payDay),
        });
      } else {
        await registerFixedExpenseDirect({
          name: form.name.trim(),
          categoryId: Number(form.categoryId),
          paymentMethod: form.method,
          merchantAliasId: null,
          newMerchantAliasName: isCard ? form.merchantName.trim() : null,
          expectedAmount: Number(form.amount),
          expectedCurrency: form.currency,
          expectedPayDay: Number(form.payDay),
        });
      }
      await onSaved();
      onClose();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "고정지출을 등록하지 못했어요."));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fx-modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section
        className="fx-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="fx-register-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="fx-modal-head">
          <div>
            <h2 id="fx-register-title">
              {candidate ? "추천 후보 등록" : "고정지출 등록"}
            </h2>
            <p>매달 반복되는 금액과 결제일을 입력해주세요.</p>
          </div>
          <button
            type="button"
            className="fx-modal-close"
            onClick={onClose}
            aria-label="닫기"
          >
            <DashboardIcon name="x" />
          </button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="fx-modal-body">
            <div className="fx-form-grid">
              <div className="fx-field full">
                <label htmlFor="fx-expense-name">고정지출 이름 *</label>
                <input
                  id="fx-expense-name"
                  value={form.name}
                  onChange={update("name")}
                  required
                  placeholder="예: 넷플릭스, 월세"
                />
              </div>
              <div className="fx-field full">
                <span className="fx-field-label">결제수단 *</span>
                <div className="fx-method-toggle">
                  <label className="fx-method-option">
                    <input
                      type="radio"
                      name="fx-method"
                      value="CARD"
                      checked={isCard}
                      disabled={Boolean(candidate)}
                      onChange={update("method")}
                    />
                    <span>
                      <DashboardIcon name="card" size={15} />
                      카드
                    </span>
                  </label>
                  <label className="fx-method-option">
                    <input
                      type="radio"
                      name="fx-method"
                      value="TRANSFER"
                      checked={!isCard}
                      disabled={Boolean(candidate)}
                      onChange={update("method")}
                    />
                    <span>
                      <DashboardIcon name="repeat" size={15} />
                      계좌이체
                    </span>
                  </label>
                </div>
              </div>
              {isCard && (
                <div className="fx-field full">
                  <label htmlFor="fx-merchant-name">서비스/가맹점 *</label>
                  <input
                    id="fx-merchant-name"
                    value={form.merchantName}
                    onChange={update("merchantName")}
                    required
                    placeholder="새 서비스 이름을 입력하세요"
                  />
                </div>
              )}
              <div className="fx-field">
                <label htmlFor="fx-category">카테고리 *</label>
                <select
                  id="fx-category"
                  value={form.categoryId}
                  onChange={update("categoryId")}
                  required
                >
                  <option value="">선택</option>
                  {(categories || []).map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="fx-field">
                <label htmlFor="fx-pay-day">매월 결제일 *</label>
                <select
                  id="fx-pay-day"
                  value={form.payDay}
                  onChange={update("payDay")}
                  required
                >
                  <option value="">선택</option>
                  {PAY_DAYS.map((day) => (
                    <option key={day} value={day}>
                      {day}일
                    </option>
                  ))}
                </select>
              </div>
              <div className="fx-field full">
                <label htmlFor="fx-amount">예상 금액 *</label>
                <div className="fx-input-group">
                  <input
                    id="fx-amount"
                    inputMode="decimal"
                    value={
                      form.amount && form.currency === "KRW"
                        ? WON.format(Number(form.amount))
                        : form.amount
                    }
                    onChange={handleAmount}
                    required
                    placeholder="0"
                  />
                  <select
                    aria-label="통화"
                    value={form.currency}
                    onChange={update("currency")}
                  >
                    <option value="KRW">KRW</option>
                    <option value="USD">USD</option>
                  </select>
                </div>
              </div>
            </div>
            <div className="fx-form-notice">
              <DashboardIcon name="info" size={15} />
              <span>
                {isCard
                  ? "카드 고정지출은 서비스 정보가 있어야 다음 결제내역과 자동으로 연결할 수 있어요."
                  : "계좌이체 고정지출은 카드내역과 대조하지 않고 결제 예정일만 알려드려요."}
              </span>
            </div>
            {error && (
              <div className="fx-form-notice fx-form-error">
                <DashboardIcon name="info" size={15} />
                <span>{error}</span>
              </div>
            )}
          </div>
          <div className="fx-modal-actions">
            <button type="button" className="fx-ghost-button" onClick={onClose}>
              취소
            </button>
            <button
              type="submit"
              className="fx-primary-button"
              disabled={isSubmitting || !categories?.length}
            >
              {isSubmitting ? "등록 중..." : "등록하기"}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}

export default RegisterExpenseModal;
