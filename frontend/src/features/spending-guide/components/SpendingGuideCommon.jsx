import DashboardIcon from "../../../components/common/DashboardIcon";

function EmptyState({ icon, title, description }) {
  return (
    <div className="spending-empty">
      <span className="spending-empty-icon">
        <DashboardIcon name={icon} size={22} />
      </span>
      <strong>{title}</strong>
      <p>{description}</p>
    </div>
  );
}

function LoadingState({ label = "데이터를 불러오고 있어요." }) {
  return (
    <div className="spending-loading">
      <span className="spending-spinner" /> {label}
    </div>
  );
}

function SectionError({ message }) {
  return (
    <div className="spending-section-error">
      <DashboardIcon name="info" size={18} /> {message}
    </div>
  );
}

function PanelTitle({ icon, title, description, tone = "green" }) {
  return (
    <div className="spending-panel-title">
      <span className={`spending-panel-icon ${tone}`}>
        <DashboardIcon name={icon} size={18} />
      </span>
      <div>
        <h2>{title}</h2>
        <p>{description}</p>
      </div>
    </div>
  );
}

export { EmptyState, LoadingState, PanelTitle, SectionError };
