import { useEffect, useMemo, useState } from "react";
import { EMPLOYMENT_STATUS_LABELS } from "../hooks/usePolicyRecommendations";

function PolicyRecommendationProfileForm({
  profile,
  regions,
  isSaving,
  submitLabel,
  onCancel,
  onSubmit,
}) {
  const [sido, setSido] = useState(profile?.residenceSido || "");
  const [sigungu, setSigungu] = useState(profile?.residenceSigungu || "");
  const [employmentStatus, setEmploymentStatus] = useState(
    profile?.employmentStatus || "",
  );
  const [formError, setFormError] = useState("");

  useEffect(() => {
    const timer = setTimeout(() => {
      setSido(profile?.residenceSido || "");
      setSigungu(profile?.residenceSigungu || "");
      setEmploymentStatus(profile?.employmentStatus || "");
    }, 0);
    return () => clearTimeout(timer);
  }, [profile]);

  const selectedRegion = useMemo(
    () => regions.find((item) => item.sido === sido),
    [regions, sido],
  );
  const sigunguOptions = selectedRegion?.sigungu || [];
  const sigunguRequired = sigunguOptions.length > 0;

  const handleSidoChange = (event) => {
    setSido(event.target.value);
    setSigungu("");
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!sido) {
      setFormError("거주 시·도를 선택해 주세요.");
      return;
    }
    if (sigunguRequired && !sigungu) {
      setFormError("거주 시·군·구를 선택해 주세요.");
      return;
    }
    if (!employmentStatus) {
      setFormError("취업 상태를 선택해 주세요.");
      return;
    }
    setFormError("");
    try {
      await onSubmit({
        residenceSido: sido,
        residenceSigungu: sigungu || null,
        employmentStatus,
      });
    } catch {
      // 서버 오류 문구는 Hook의 mutationError로 표시한다.
    }
  };

  return (
    <form className="policy-recommendation-form" onSubmit={handleSubmit}>
      <div className="policy-recommendation-age">
        <span>적용 나이</span>
        <strong>만 {profile?.age ?? "-"}세</strong>
        <p>회원가입 시 입력한 생년월일을 기준으로 자동 계산됩니다.</p>
      </div>
      <label>
        거주 시·도
        <select value={sido} onChange={handleSidoChange} disabled={isSaving}>
          <option value="">선택</option>
          {regions.map((item) => (
            <option key={item.sido} value={item.sido}>
              {item.sido}
            </option>
          ))}
        </select>
      </label>
      <label>
        거주 시·군·구
        <select
          value={sigungu}
          onChange={(event) => setSigungu(event.target.value)}
          disabled={isSaving || !sido || !sigunguRequired}
        >
          <option value="">{sigunguRequired ? "선택" : "해당 없음"}</option>
          {sigunguOptions.map((item) => (
            <option key={item} value={item}>
              {item}
            </option>
          ))}
        </select>
      </label>
      <label>
        취업 상태
        <select
          value={employmentStatus}
          onChange={(event) => setEmploymentStatus(event.target.value)}
          disabled={isSaving}
        >
          <option value="">선택</option>
          {Object.entries(EMPLOYMENT_STATUS_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </label>
      {formError && <p className="policy-recommendation-error">{formError}</p>}
      <div className="policy-recommendation-form-actions">
        {onCancel && (
          <button
            type="button"
            className="mp-ghost-button"
            disabled={isSaving}
            onClick={onCancel}
          >
            취소
          </button>
        )}
        <button type="submit" className="policy-primary-button" disabled={isSaving}>
          {isSaving ? "저장 중..." : submitLabel}
        </button>
      </div>
    </form>
  );
}

export default PolicyRecommendationProfileForm;
