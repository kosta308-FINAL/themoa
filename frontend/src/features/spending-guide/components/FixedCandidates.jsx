import { Link } from "react-router-dom";
import DashboardIcon from "../../../components/common/DashboardIcon";
import { formatWon } from "../spendingGuideUtils";
import { EmptyState, LoadingState, SectionError } from "./SpendingGuideCommon";

function FixedCandidates({ data, error }) {
  if (error) return <SectionError message={error} />;
  if (!data) return <LoadingState />;
  if (!data.length)
    return (
      <EmptyState
        icon="repeat"
        title="아직 발견된 고정지출 후보가 없어요"
        description="거래가 쌓이면 반복 결제 패턴을 이곳에서 확인할 수 있어요."
      />
    );
  return (
    <div className="spending-candidate-list">
      {data.slice(0, 3).map((candidate) => (
        <div key={candidate.id}>
          <span className="spending-candidate-icon">
            <DashboardIcon name="card" size={17} />
          </span>
          <div>
            <strong>{candidate.merchantAliasName}</strong>
            <p>
              매달 약 {formatWon(candidate.avgAmount)} · {candidate.avgPayDay}
              일쯤
            </p>
          </div>
          <Link to="/dashboard/fixed-expenses">등록</Link>
        </div>
      ))}
    </div>
  );
}

export default FixedCandidates;
