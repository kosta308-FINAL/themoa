import DashboardIcon from "../../../components/common/DashboardIcon";

function ComingSoonCard() {
  return (
    <section className="mp-card mp-coming-soon">
      <div className="mp-card-head">
        <span className="mp-card-icon">
          <DashboardIcon name="sparkle" size={17} />
        </span>
        <h2>북마크 · 금융성향진단</h2>
      </div>
      <p className="mp-empty">
        정책·금융상품 북마크와 금융성향진단 이력은 준비 중이에요. 곧 만나보실 수
        있어요.
      </p>
    </section>
  );
}

export default ComingSoonCard;
