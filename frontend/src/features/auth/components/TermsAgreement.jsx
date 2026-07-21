import { useState } from "react";
import { TERMS } from "../constants/terms";

/**
 * 회원가입 3단계 — 약관 동의.
 * @param {{ agreements: Record<string, boolean>, onChange: (key: string, value: boolean) => void }} props
 */
function TermsAgreement({ agreements, onChange }) {
  const [expandedKey, setExpandedKey] = useState(null);

  const allChecked = TERMS.every((t) => agreements[t.key]);

  const handleAllToggle = () => {
    const next = !allChecked;
    TERMS.forEach((t) => onChange(t.key, next));
  };

  return (
    <div className="auth-terms">
      <label className="auth-terms-all">
        <input
          type="checkbox"
          checked={allChecked}
          onChange={handleAllToggle}
        />
        <span>전체 동의</span>
      </label>

      <ul className="auth-terms-list">
        {TERMS.map((term) => (
          <li key={term.key} className="auth-term">
            <div className="auth-term-row">
              <label className="auth-term-check">
                <input
                  type="checkbox"
                  checked={!!agreements[term.key]}
                  onChange={(e) => onChange(term.key, e.target.checked)}
                />
                <span>
                  <span
                    className={
                      term.required
                        ? "auth-term-badge required"
                        : "auth-term-badge optional"
                    }
                  >
                    {term.required ? "필수" : "선택"}
                  </span>
                  {term.label}
                </span>
              </label>
              <button
                type="button"
                className="auth-term-view"
                onClick={() =>
                  setExpandedKey((cur) => (cur === term.key ? null : term.key))
                }
                aria-expanded={expandedKey === term.key}
              >
                {expandedKey === term.key ? "접기" : "보기"}
              </button>
            </div>
            {expandedKey === term.key && (
              <pre className="auth-term-body">{term.body}</pre>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}

export default TermsAgreement;
