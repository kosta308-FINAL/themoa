import { useEffect, useState } from "react";
import {
  getSearchSettings,
  resetSearchSettings,
  updateSearchSettings,
} from "../../../api/financialAdminApi";
import { getApiErrorMessage } from "../../../utils/apiError";

const toFormValues = (data) => ({
  topK: String(data?.topK ?? ""),
  retryTopK: String(data?.retryTopK ?? ""),
  minimumSimilarity: String(data?.minimumSimilarity ?? ""),
});

/**
 * 검색 튜닝값 조회·변경 상태.
 * 서버가 PUT 응답으로 "실제 적용된 값"을 돌려주므로 저장 후 재조회하지 않고 그 값을 그대로 반영한다.
 * (허용 범위를 벗어난 입력은 서버가 범위 안으로 조정하므로, 입력값이 아니라 응답값을 신뢰해야 한다)
 */
export const useSearchSettings = () => {
  const [settings, setSettings] = useState(null);
  const [form, setForm] = useState(toFormValues(null));
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [resetting, setResetting] = useState(false);
  const [error, setError] = useState("");

  const applySettings = (data) => {
    setSettings(data || null);
    setForm(toFormValues(data));
  };

  useEffect(() => {
    let active = true;
    getSearchSettings()
      .then((data) => {
        if (active) {
          applySettings(data);
        }
      })
      .catch((loadError) => {
        if (active) {
          setError(
            getApiErrorMessage(loadError, "검색 설정을 불러오지 못했어요."),
          );
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, []);

  const setField = (field, value) =>
    setForm((prev) => ({ ...prev, [field]: value }));

  /** 빈 칸은 "이 항목은 서버 기본값으로" 라는 뜻이라 null로 보낸다. */
  const toPayloadValue = (value, parse) => {
    const trimmed = String(value).trim();
    if (trimmed === "") {
      return null;
    }
    const parsed = parse(trimmed);
    return Number.isNaN(parsed) ? null : parsed;
  };

  const save = async () => {
    setSaving(true);
    setError("");
    try {
      const updated = await updateSearchSettings({
        topK: toPayloadValue(form.topK, Number.parseInt),
        retryTopK: toPayloadValue(form.retryTopK, Number.parseInt),
        minimumSimilarity: toPayloadValue(
          form.minimumSimilarity,
          Number.parseFloat,
        ),
      });
      applySettings(updated);
      return true;
    } catch (saveError) {
      setError(getApiErrorMessage(saveError, "검색 설정 변경에 실패했어요."));
      return false;
    } finally {
      setSaving(false);
    }
  };

  /** 저장해 둔 값을 지우고 서버 기본값으로 되돌린다. 응답이 곧 기본값이라 그대로 반영한다. */
  const reset = async () => {
    setResetting(true);
    setError("");
    try {
      applySettings(await resetSearchSettings());
      return true;
    } catch (resetError) {
      setError(getApiErrorMessage(resetError, "초기값으로 되돌리지 못했어요."));
      return false;
    } finally {
      setResetting(false);
    }
  };

  return {
    settings,
    form,
    loading,
    saving,
    resetting,
    error,
    setField,
    save,
    reset,
  };
};
