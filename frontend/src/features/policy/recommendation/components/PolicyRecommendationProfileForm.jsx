import { useEffect, useMemo, useState } from "react";
import DashboardIcon from "../../../../components/common/DashboardIcon";
import OptionPicker from "../../../../components/common/OptionPicker";
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
  const [isSidoOpen, setIsSidoOpen] = useState(false);
  const [isSigunguOpen, setIsSigunguOpen] = useState(false);
  const [isEmploymentOpen, setIsEmploymentOpen] = useState(false);

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
  const sigunguOptions = useMemo(
    () => selectedRegion?.sigungu || [],
    [selectedRegion],
  );
  const sigunguRequired = sigunguOptions.length > 0;

  const sidoOptions = useMemo(
    () => regions.map((item) => ({ value: item.sido, label: item.sido })),
    [regions],
  );
  const sigunguPickerOptions = useMemo(
    () => sigunguOptions.map((item) => ({ value: item, label: item })),
    [sigunguOptions],
  );
  const employmentOptions = useMemo(
    () =>
      Object.entries(EMPLOYMENT_STATUS_LABELS).map(([value, label]) => ({
        value,
        label,
      })),
    [],
  );

  const handleSidoSelect = (value) => {
    setSido(value);
    setSigungu("");
    setIsSidoOpen(false);
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
        <div className="policy-recommendation-age-badge">
          <span>적용 나이</span>
          <strong>만 {profile?.age ?? "-"}세</strong>
        </div>
        <p>회원가입 시 입력한 생년월일을 기준으로 자동 계산됩니다.</p>
      </div>
      <div className="policy-recommendation-fields">
        <div className="policy-recommendation-select">
          <span>거주 시·도</span>
          <div className="policy-recommendation-select-field">
            <button
              type="button"
              className="policy-recommendation-select-trigger"
              disabled={isSaving}
              onClick={() => setIsSidoOpen((open) => !open)}
            >
              {sido || "선택"}
              <DashboardIcon name="chevron-down" size={13} />
            </button>
            {isSidoOpen && (
              <OptionPicker
                options={sidoOptions}
                value={sido}
                title="거주 시·도 선택"
                onSelect={handleSidoSelect}
                onClose={() => setIsSidoOpen(false)}
              />
            )}
          </div>
        </div>
        <div className="policy-recommendation-select">
          <span>거주 시·군·구</span>
          <div className="policy-recommendation-select-field">
            <button
              type="button"
              className="policy-recommendation-select-trigger"
              disabled={isSaving || !sido || !sigunguRequired}
              onClick={() => setIsSigunguOpen((open) => !open)}
            >
              {sigungu || (sigunguRequired ? "선택" : "해당 없음")}
              <DashboardIcon name="chevron-down" size={13} />
            </button>
            {isSigunguOpen && (
              <OptionPicker
                options={sigunguPickerOptions}
                value={sigungu}
                title="거주 시·군·구 선택"
                onSelect={(value) => {
                  setSigungu(value);
                  setIsSigunguOpen(false);
                }}
                onClose={() => setIsSigunguOpen(false)}
              />
            )}
          </div>
        </div>
        <div className="policy-recommendation-select">
          <span>취업 상태</span>
          <div className="policy-recommendation-select-field">
            <button
              type="button"
              className="policy-recommendation-select-trigger"
              disabled={isSaving}
              onClick={() => setIsEmploymentOpen((open) => !open)}
            >
              {EMPLOYMENT_STATUS_LABELS[employmentStatus] || "선택"}
              <DashboardIcon name="chevron-down" size={13} />
            </button>
            {isEmploymentOpen && (
              <OptionPicker
                options={employmentOptions}
                value={employmentStatus}
                title="취업 상태 선택"
                onSelect={(value) => {
                  setEmploymentStatus(value);
                  setIsEmploymentOpen(false);
                }}
                onClose={() => setIsEmploymentOpen(false)}
              />
            )}
          </div>
        </div>
      </div>
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
        <button
          type="submit"
          className="policy-primary-button"
          disabled={isSaving}
        >
          {isSaving ? "저장 중..." : submitLabel}
        </button>
      </div>
    </form>
  );
}

export default PolicyRecommendationProfileForm;
