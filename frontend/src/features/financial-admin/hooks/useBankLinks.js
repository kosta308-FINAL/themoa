import { useEffect, useState } from "react";
import {
  deleteBankLink,
  getBankLinks,
  saveBankLink,
} from "../../../api/financialAdminApi";
import { getApiErrorMessage } from "../../../utils/apiError";

/** 사용자에게 그대로 노출되는 링크라 서버와 같은 기준(http/https)으로 미리 걸러 안내한다. */
const isValidUrl = (url) => /^https?:\/\//.test(url.trim());

/**
 * 은행 공식 링크 관리 상태.
 * 등록·삭제 후에는 목록을 다시 받아온다 — companiesWithoutLink(링크 없는 회사) 판정이
 * 서버의 부분 매칭 로직으로 계산되므로 클라이언트에서 흉내 내지 않는다.
 */
export const useBankLinks = () => {
  const [links, setLinks] = useState([]);
  const [companiesWithoutLink, setCompaniesWithoutLink] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [busyCompany, setBusyCompany] = useState("");

  const applyData = (data) => {
    setLinks(data?.links || []);
    setCompaniesWithoutLink(data?.companiesWithoutLink || []);
  };

  useEffect(() => {
    let active = true;
    getBankLinks()
      .then((data) => {
        if (active) {
          applyData(data);
        }
      })
      .catch((loadError) => {
        if (active) {
          setError(
            getApiErrorMessage(loadError, "은행 링크를 불러오지 못했어요."),
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

  const reload = async () => {
    try {
      applyData(await getBankLinks());
    } catch (loadError) {
      setError(getApiErrorMessage(loadError, "은행 링크를 불러오지 못했어요."));
    }
  };

  const save = async (companyName, officialUrl) => {
    const name = companyName.trim();
    const url = officialUrl.trim();
    if (!name) {
      setError("회사명을 입력해 주세요.");
      return false;
    }
    if (!isValidUrl(url)) {
      setError("링크는 http:// 또는 https:// 로 시작해야 해요.");
      return false;
    }

    setSaving(true);
    setError("");
    try {
      await saveBankLink({ companyName: name, officialUrl: url });
      await reload();
      return true;
    } catch (saveError) {
      setError(getApiErrorMessage(saveError, "링크 저장에 실패했어요."));
      return false;
    } finally {
      setSaving(false);
    }
  };

  const remove = async (companyName) => {
    setBusyCompany(companyName);
    setError("");
    try {
      await deleteBankLink(companyName);
      await reload();
    } catch (removeError) {
      setError(getApiErrorMessage(removeError, "링크 삭제에 실패했어요."));
    } finally {
      setBusyCompany("");
    }
  };

  return {
    links,
    companiesWithoutLink,
    loading,
    error,
    saving,
    busyCompany,
    save,
    remove,
  };
};
