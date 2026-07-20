import RecommendResultCard from "./RecommendResultCard";

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

/**
 * 추천 결과 영역. 로딩·오류·검색 전·빈 결과·정상 목록을 분기해서 보여준다.
 */
function RecommendResults({ loading, error, data, searched }) {
  if (loading) {
    return <div className="rec-state">추천 상품을 찾고 있어요…</div>;
  }
  if (error) {
    return <div className="rec-alert rec-alert-danger">{error}</div>;
  }
  if (!searched) {
    return (
      <div className="rec-state">
        왼쪽에서 내 정보를 입력하고 추천받기를 눌러 주세요.
      </div>
    );
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
          />
        ))
      )}
    </div>
  );
}

export default RecommendResults;
