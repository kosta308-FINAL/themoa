import { Link } from "react-router-dom";
import DashboardIcon from "../../../components/common/DashboardIcon";

function DashEmptyState({ icon = "info", title, description, action }) {
  return (
    <div className="dash-empty-state">
      <span className="dash-empty-icon">
        <DashboardIcon name={icon} size={20} />
      </span>
      {title && <strong>{title}</strong>}
      {description && <span>{description}</span>}
      {action && (
        <Link to={action.to} className="dash-empty-cta">
          {action.label}
        </Link>
      )}
    </div>
  );
}

export default DashEmptyState;
