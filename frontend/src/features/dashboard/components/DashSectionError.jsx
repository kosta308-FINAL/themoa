import DashboardIcon from "../../../components/common/DashboardIcon";

function DashSectionError({ message }) {
  return (
    <div className="dash-section-error">
      <span className="dash-section-error-icon">
        <DashboardIcon name="x" size={14} />
      </span>
      <span>{message}</span>
    </div>
  );
}

export default DashSectionError;
