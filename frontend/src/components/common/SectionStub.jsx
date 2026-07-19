import "../../features/dashboard/Dashboard.css";

function SectionStub({ title, description }) {
  return (
    <main className="dash-main">
      <div className="dash-topbar">
        <div>
          <h1>{title}</h1>
          <p>{description}</p>
        </div>
      </div>
      <div className="dash-placeholder">준비 중인 페이지입니다.</div>
    </main>
  );
}

export default SectionStub;
