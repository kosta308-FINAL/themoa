import DashboardIcon from "../../../components/common/DashboardIcon";
import { TERMS_TYPE_LABELS, formatDateTime } from "../mypageUtils";

function TermsHistoryCard({ termsAgreements }) {
  return (
    <section className="mp-card">
      <div className="mp-card-head">
        <span className="mp-card-icon">
          <DashboardIcon name="check" size={17} />
        </span>
        <h2>약관 동의 이력</h2>
      </div>
      {!termsAgreements?.length ? (
        <p className="mp-empty">동의 이력이 없어요.</p>
      ) : (
        <ul className="mp-terms-list">
          {termsAgreements.map((item, index) => (
            <li key={`${item.termsType}-${item.agreedAt}-${index}`}>
              <span>{TERMS_TYPE_LABELS[item.termsType] || item.termsType}</span>
              <span className="mp-terms-meta">
                v{item.termsVersion} · {formatDateTime(item.agreedAt)}
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export default TermsHistoryCard;
