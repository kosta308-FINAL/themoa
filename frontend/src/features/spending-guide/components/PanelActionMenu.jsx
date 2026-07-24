import { useState } from "react";
import DashboardIcon from "../../../components/common/DashboardIcon";

/**
 * 좁은 화면에서 패널 액션 버튼들이 한 줄에 다 안 들어갈 때
 * 하나의 버튼으로 모아 드롭다운 메뉴로 보여주는 컴포넌트.
 * CSS 미디어쿼리로 데스크톱에서는 숨기고 모바일에서만 노출한다.
 */
function PanelActionMenu({ actions, label = "더보기" }) {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <div className="spending-panel-menu">
      <button
        type="button"
        className="spending-panel-menu-trigger"
        aria-expanded={isOpen}
        aria-label={label}
        onClick={() => setIsOpen((open) => !open)}
      >
        <DashboardIcon name="menu" size={16} />
      </button>
      {isOpen && (
        <ul className="spending-panel-menu-dropdown" role="menu">
          {actions.map((action) => (
            <li key={action.key}>
              <button
                type="button"
                role="menuitem"
                disabled={action.disabled}
                onClick={() => {
                  setIsOpen(false);
                  action.onClick();
                }}
              >
                {action.spinning ? (
                  <span className="spending-spinner spending-spinner-sm" />
                ) : (
                  <DashboardIcon name={action.icon} size={15} />
                )}
                {action.label}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default PanelActionMenu;
