import { Link } from "react-router-dom";
import RecommendResultCard from "./RecommendResultCard";

/**
 * 추천 로딩 표시. POST /recommend가 LLM(AI 총평)까지 도느라 수 초 걸려서
 * 결과(상품이 뜨는) 영역 자리에만 스피너를 가운데 띄운다.
 */
function RecommendLoading() {
  return (
    <div className="rec-loading" role="status" aria-live="polite">
      <span className="rec-spinner" aria-hidden="true" />
      <p>AI가 딱 맞는 상품을 고르고 있어요…</p>
    </div>
  );
}

/**
 * 저축목표 실현가능성 안내. 목표를 입력했을 때(feasibility.hasGoal)만 노출한다.
 * 프론트는 한글 메시지가 아니라 백엔드가 준 플래그로 분기한다.
 */
function FeasibilityNotice({ feasibility }) {
  if (!feasibility || !feasibility.hasGoal) {
    return null;
  }
  if (feasibility.hopeless) {
    return (
      <div className="rec-alert rec-alert-danger">
        ⚠️ 이 목표는 예·적금만으로는 현실적이지 않아요. 월 납입액을 늘리거나
        목표를 낮춰 다시 시도해 주세요.
      </div>
    );
  }
  if (!feasibility.reachableAtGoalMonths) {
    return (
      <div className="rec-alert rec-alert-warn">
        ⚠️ 입력한 목표기간 안에는 목표금액을 채우기 어려워요.
        {feasibility.actualMonthsNeeded != null &&
          ` 실제로는 약 ${feasibility.actualMonthsNeeded}개월이 필요해요.`}
      </div>
    );
  }
  return (
    <div className="rec-alert rec-alert-ok">
      ✅ 입력한 목표기간 안에 목표금액 달성이 가능해요.
    </div>
  );
}

function RecommendIntroState() {
  return (
    <div className="rec-empty">
      <div className="rec-empty-content">
        <span className="rec-empty-kicker">추천 준비</span>
        <h2>가입 가능한 적금을 조건에 맞춰 골라드려요.</h2>
        <p>
          월 납입금액, 선호 기간, 우대조건 수용 여부를 반영해 예상 만기금액과
          추천 이유를 함께 보여드릴게요.
        </p>
        <ul className="rec-empty-points">
          <li>가입 가능 조건 확인</li>
          <li>예상 만기금액 비교</li>
          <li>AI 추천 이유 정리</li>
        </ul>
        <Link to="/dashboard/products/search" className="rec-empty-link">
          상품명으로 먼저 찾아보기
        </Link>
      </div>

      <div className="rec-empty-preview" aria-hidden="true">
        <div className="rec-empty-preview-head">
          <span>추천 결과 미리보기</span>
          <strong>TOP 3</strong>
        </div>
        <div className="rec-empty-preview-row">
          <span className="rec-empty-rank">1</span>
          <span className="rec-empty-product">
            <strong>우대금리 높은 적금</strong>
            <small>조건 충족 가능성이 높아요</small>
          </span>
          <span className="rec-empty-rate">4.2%</span>
        </div>
        <div className="rec-empty-preview-row">
          <span className="rec-empty-rank">2</span>
          <span className="rec-empty-product">
            <strong>단기 목표형 적금</strong>
            <small>선호 기간에 잘 맞아요</small>
          </span>
          <span className="rec-empty-rate">3.8%</span>
        </div>
        <div className="rec-empty-preview-row muted">
          <span className="rec-empty-rank">3</span>
          <span className="rec-empty-product">
            <strong>중도해지 부담 낮은 상품</strong>
            <small>유동성을 고려해요</small>
          </span>
          <span className="rec-empty-rate">3.5%</span>
        </div>
      </div>
    </div>
  );
}

/**
 * 추천 결과 영역. 로딩·오류·검색 전·빈 결과·정상 목록을 분기해서 보여준다.
 */
function RecommendResults({
  loading,
  error,
  data,
  searched,
  bookmarks,
  depositWon,
}) {
  if (loading) {
    return <RecommendLoading />;
  }
  if (error) {
    return <div className="rec-alert rec-alert-danger">{error}</div>;
  }
  if (!searched) {
    return <RecommendIntroState />;
  }

  const recommendations = data?.recommendations || [];

  return (
    <div className="rec-results">
      <FeasibilityNotice feasibility={data?.feasibility} />
      {recommendations.length === 0 ? (
        <div className="rec-state">
          조건에 맞는 상품이 없어요. 입력을 바꿔서 다시 시도해 보세요.
        </div>
      ) : (
        recommendations.map((item, index) => (
          <RecommendResultCard
            key={`${item.company}-${item.productName}-${index}`}
            item={item}
            rank={index + 1}
            bookmarks={bookmarks}
            depositWon={depositWon}
          />
        ))
      )}
    </div>
  );
}

export default RecommendResults;
