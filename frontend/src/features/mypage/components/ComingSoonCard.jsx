import DashboardIcon from "../../../components/common/DashboardIcon";

function ComingSoonCard() {
  return (
    <section className="mp-card mp-coming-soon">
      <div className="mp-card-head">
        <span className="mp-card-icon">
          <DashboardIcon name="sparkle" size={17} />
        </span>
        <h2>추가 기능 준비 중</h2>
      </div>
      <p className="mp-empty">
        금융상품 북마크와 금융성향진단 이력은 준비 중이에요.
      </p>
    </section>
  );
}

export default ComingSoonCard;
