import { useRef, useState } from "react";
import DashboardIcon from "../../../components/common/DashboardIcon";
import { getApiErrorMessage } from "../../../utils/apiError";
import {
  confirmMissedPayment,
  registerFixedExpenseDirect,
  registerFixedExpenseFromCandidate,
} from "../../../api/fixedExpenseApi";
import MerchantAliasPicker from "./MerchantAliasPicker";

const PAY_DAYS = Array.from({ length: 31 }, (_, index) => index + 1);
const WON = new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 0 });

function RegisterExpenseModal({
  candidate = null,
  initial = null,
  categories,
  onClose,
  onSaved,
  onStale,
}) {
  const [form, setForm] = useState({
    name: candidate?.merchantAliasName || initial?.name || "",
    method: candidate ? "CARD" : initial?.method || "CARD",
    merchantAliasId: initial?.merchantAliasId ?? null,
    merchantName: candidate?.merchantAliasName || initial?.merchantName || "",
    categoryId: candidate?.recommendedCategoryId
      ? String(candidate.recommendedCategoryId)
      : initial?.categoryId || "",
    payDay: candidate?.avgPayDay
      ? String(candidate.avgPayDay)
      : initial?.payDay || "",
    amount: candidate?.avgAmount
      ? String(Math.round(Number(candidate.avgAmount)))
      : initial?.amount || "",
    currency: "KRW",
  });
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const isSubmittingRef = useRef(false);

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
    if (isSubmittingRef.current) return;
    isSubmittingRef.current = true;
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
        const created = await registerFixedExpenseDirect({
          name: form.name.trim(),
          categoryId: Number(form.categoryId),
          paymentMethod: form.method,
          merchantAliasId: isCard ? form.merchantAliasId : null,
          newMerchantAliasName:
            isCard && !form.merchantAliasId ? form.merchantName.trim() : null,
          expectedAmount: Number(form.amount),
          expectedCurrency: form.currency,
          expectedPayDay: Number(form.payDay),
        });
        if (initial?.transactionId) {
          // 실거래에서 등록한 경우 그 거래를 바로 이번 달 이행으로 확정한다 —
          // 안 그러면 방금 쓴 데이터로 만든 항목이 "미납"으로 보이는 모순이 생긴다.
          try {
            await confirmMissedPayment(created.id, initial.transactionId);
          } catch {
            // 등록 자체는 이미 성공했으니 확정 실패는 조용히 넘어간다(F-05에서 다시 시도 가능).
          }
        }
      }
      await onSaved();
      onClose();
    } catch (requestError) {
      if (
        candidate &&
        requestError?.response?.data?.code ===
          "FIXED_EXPENSE_CANDIDATE_NOT_PENDING"
      ) {
        // 이미 처리된(중복 요청 등) 후보 - 에러로 가두지 않고 최신 상태로 정리한다.
        await onStale();
        onClose();
        return;
      }
      setError(
        getApiErrorMessage(requestError, "고정지출을 등록하지 못했어요."),
      );
      isSubmittingRef.current = false;
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      className="fx-modal-backdrop"
      role="presentation"
      onMouseDown={onClose}
    >
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
              {candidate
                ? "추천 후보 등록"
                : initial
                  ? "거래로 고정지출 등록"
                  : "고정지출 등록"}
            </h2>
            <p>
              {initial
                ? "선택한 거래 정보를 바탕으로 채웠어요. 결제일을 확인해주세요."
                : "매달 반복되는 금액과 결제일을 입력해주세요."}
            </p>
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
                      disabled={Boolean(candidate || initial)}
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
                      disabled={Boolean(candidate || initial)}
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
                  <span className="fx-field-label">서비스/가맹점 *</span>
                  {candidate || (initial && form.merchantAliasId) ? (
                    <input value={form.merchantName} readOnly />
                  ) : (
                    <MerchantAliasPicker
                      initialName={form.merchantName}
                      onChange={({ merchantAliasId, name }) =>
                        setForm((current) => ({
                          ...current,
                          merchantAliasId,
                          merchantName: name,
                        }))
                      }
                    />
                  )}
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
