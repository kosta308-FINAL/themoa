import { Link } from "react-router-dom";
import { useCallback, useEffect, useState } from "react";
import DashboardIcon from "../../components/common/DashboardIcon";
import { getApiErrorMessage } from "../../utils/apiError";
import {
  getCategories,
  getFixedExpenseCandidates,
  getSpendingGuideSummary,
} from "../../api/spendingGuideApi";
import {
  getFixedExpenses,
  reclassifyFixedExpenseCandidateAsHabit,
  rejectFixedExpenseCandidate,
  snoozeFixedExpenseCandidate,
} from "../../api/fixedExpenseApi";
import FixedExpenseSuggestions from "./components/FixedExpenseSuggestions";
import FixedExpenseList from "./components/FixedExpenseList";
import UpcomingPayments from "./components/UpcomingPayments";
import RegisterExpenseModal from "./components/RegisterExpenseModal";
import ExpenseDetailModal from "./components/ExpenseDetailModal";
import { formatWon, toNumber } from "./fixedExpenseUtils";
import "./FixedExpensePage.css";

function FixedExpensePage() {
  const [expenseList, setExpenseList] = useState(null);
  const [candidates, setCandidates] = useState([]);
  const [categories, setCategories] = useState([]);
  const [salarySummary, setSalarySummary] = useState(null);
  const [pageError, setPageError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [filter, setFilter] = useState("all");
  const [sort, setSort] = useState("payday");
  const [pendingCandidateId, setPendingCandidateId] = useState(null);
  const [registerState, setRegisterState] = useState(null); // { candidate } | { candidate: null }
  const [detailExpense, setDetailExpense] = useState(null);
  const [toast, setToast] = useState("");

  const showToast = (message) => setToast(message);

  useEffect(() => {
    if (!toast) return undefined;
    const timer = setTimeout(() => setToast(""), 2200);
    return () => clearTimeout(timer);
  }, [toast]);

  const load = useCallback(async () => {
    setIsLoading(true);
    setPageError("");
    try {
      const [
        expensesResult,
        candidatesResult,
        categoriesResult,
        summaryResult,
      ] = await Promise.allSettled([
        getFixedExpenses(),
        getFixedExpenseCandidates(),
        getCategories(),
        getSpendingGuideSummary(),
      ]);
      if (expensesResult.status === "fulfilled")
        setExpenseList(expensesResult.value);
      else
        setPageError(
          getApiErrorMessage(
            expensesResult.reason,
            "고정지출을 불러오지 못했어요.",
          ),
        );
      setCandidates(
        candidatesResult.status === "fulfilled" ? candidatesResult.value : [],
      );
      setCategories(
        categoriesResult.status === "fulfilled" ? categoriesResult.value : [],
      );
      setSalarySummary(
        summaryResult.status === "fulfilled" ? summaryResult.value : null,
      );
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    const run = () => load();
    run();
  }, [load]);

  const reloadCandidates = async () => {
    try {
      setCandidates(await getFixedExpenseCandidates());
    } catch {
      // 목록 갱신 실패는 다음 새로고침에서 다시 시도한다.
    }
  };

  const handleCandidateSnooze = async (candidateId) => {
    setPendingCandidateId(candidateId);
    try {
      await snoozeFixedExpenseCandidate(candidateId);
      await reloadCandidates();
      showToast("이번 주기에는 추천을 잠시 미뤘어요.");
    } catch (error) {
      showToast(getApiErrorMessage(error, "요청을 처리하지 못했어요."));
    } finally {
      setPendingCandidateId(null);
    }
  };

  const handleCandidateReject = async (candidateId) => {
    setPendingCandidateId(candidateId);
    try {
      await rejectFixedExpenseCandidate(candidateId);
      await reloadCandidates();
      showToast("앞으로 이 항목은 다시 추천하지 않을게요.");
    } catch (error) {
      showToast(getApiErrorMessage(error, "요청을 처리하지 못했어요."));
    } finally {
      setPendingCandidateId(null);
    }
  };

  const handleCandidateReclassifyHabit = async (candidateId) => {
    setPendingCandidateId(candidateId);
    try {
      await reclassifyFixedExpenseCandidateAsHabit(candidateId);
      await reloadCandidates();
      showToast("습관적 소비로 분류했어요.");
    } catch (error) {
      showToast(getApiErrorMessage(error, "요청을 처리하지 못했어요."));
    } finally {
      setPendingCandidateId(null);
    }
  };

  const handleRegisterSaved = async () => {
    await load();
    showToast("고정지출을 등록했어요.");
  };

  const handleRegisterStale = async () => {
    await load();
    showToast("이미 처리된 추천 후보예요. 최신 상태로 갱신했어요.");
  };

  const handleDetailChanged = async () => {
    await load();
  };

  const items = expenseList?.items || [];
  const salaryAmount = toNumber(salarySummary?.salaryAmount);
  const totalExpected = toNumber(expenseList?.totalExpectedAmountKrw);
  const ratio = salaryAmount > 0 ? (totalExpected / salaryAmount) * 100 : null;

  return (
    <div className="fixed-expense">
      <main className="fx-page">
        <div className="fx-page-head">
          <div>
            <h1>고정지출</h1>
            <p>매달 반복되는 지출과 결제 상태를 한곳에서 관리해보세요.</p>
          </div>
          <button
            type="button"
            className="fx-primary-button"
            onClick={() => setRegisterState({ candidate: null })}
          >
            <DashboardIcon name="plus" size={15} />
            고정지출 등록
          </button>
        </div>

        {pageError && (
          <div className="fx-page-error">
            <DashboardIcon name="info" size={18} />
            <span>{pageError}</span>
            <button type="button" onClick={load}>
              다시 시도
            </button>
          </div>
        )}

        {isLoading && !expenseList ? (
          <div className="fx-loading">고정지출을 불러오고 있어요.</div>
        ) : (
          <>
            <section className="fx-summary-hero" aria-label="월 고정지출 요약">
              <article className="fx-summary-main">
                <span className="fx-summary-label">
                  <DashboardIcon name="repeat" size={15} />월 예상 고정지출
                </span>
                <div className="fx-summary-total">
                  <strong>{formatWon(totalExpected)}</strong>
                  <span>총 {expenseList?.count || 0}건</span>
                </div>
                <p className="fx-summary-caption">
                  등록된 고정지출 {expenseList?.count || 0}건의 원화 예상
                  금액이에요.
                </p>
                <div className="fx-summary-stats">
                  <div>
                    <span>등록 항목</span>
                    <strong>{items.length}건</strong>
                  </div>
                  <div>
                    <span>카드 결제</span>
                    <strong>
                      {
                        items.filter((item) => item.paymentMethod === "CARD")
                          .length
                      }
                      건
                    </strong>
                  </div>
                  <div>
                    <span>계좌이체</span>
                    <strong>
                      {
                        items.filter(
                          (item) => item.paymentMethod === "TRANSFER",
                        ).length
                      }
                      건
                    </strong>
                  </div>
                </div>
              </article>
              <aside className="fx-summary-side">
                {ratio !== null ? (
                  <>
                    <div className="fx-salary-head">
                      <span>급여 대비 고정지출</span>
                      <strong>{ratio.toFixed(1)}%</strong>
                    </div>
                    <span className="fx-salary-base">
                      월 급여 {formatWon(salaryAmount)} 기준
                    </span>
                    <div
                      className="fx-progress-track"
                      role="progressbar"
                      aria-label="급여 대비 고정지출 비율"
                      aria-valuemin={0}
                      aria-valuemax={100}
                      aria-valuenow={Math.min(100, Math.round(ratio))}
                    >
                      <div
                        className="fx-progress-value"
                        style={{ width: `${Math.min(100, ratio)}%` }}
                      />
                    </div>
                    <p className="fx-ratio-help">
                      급여 중 {formatWon(totalExpected)}이 고정지출로 먼저
                      나가요.
                    </p>
                  </>
                ) : (
                  <div className="fx-salary-empty">
                    <strong>급여 정보가 없어요</strong>
                    <p>월급을 등록하면 급여 대비 고정지출 비율을 보여드려요.</p>
                    <Link
                      className="fx-secondary-button"
                      to="/dashboard/spending"
                    >
                      소비가이드에서 등록하기
                    </Link>
                  </div>
                )}
              </aside>
            </section>

            <div className="fx-content-grid">
              <FixedExpenseList
                items={items}
                filter={filter}
                onFilterChange={setFilter}
                sort={sort}
                onSortChange={setSort}
                onSelect={setDetailExpense}
              />
              <div className="fx-side-col">
                <FixedExpenseSuggestions
                  candidates={candidates}
                  pendingId={pendingCandidateId}
                  onRegister={(candidate) => setRegisterState({ candidate })}
                  onSnooze={handleCandidateSnooze}
                  onReject={handleCandidateReject}
                  onReclassifyHabit={handleCandidateReclassifyHabit}
                />
                <UpcomingPayments items={items} />
              </div>
            </div>
          </>
        )}
      </main>

      {registerState && (
        <RegisterExpenseModal
          candidate={registerState.candidate}
          categories={categories}
          onClose={() => setRegisterState(null)}
          onSaved={handleRegisterSaved}
          onStale={handleRegisterStale}
        />
      )}

      {detailExpense && (
        <ExpenseDetailModal
          expense={detailExpense}
          onClose={() => setDetailExpense(null)}
          onChanged={handleDetailChanged}
        />
      )}

      {toast && (
        <div className="fx-toast show" role="status" aria-live="polite">
          {toast}
        </div>
      )}
    </div>
  );
}

export default FixedExpensePage;
