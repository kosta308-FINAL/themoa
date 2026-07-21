import { useEffect } from "react";
import "./Toast.css";

const AUTO_HIDE_MS = 2000;

/**
 * 화면 하단 중앙에 잠깐 떴다 사라지는 안내 문구.
 * toast는 {message, id} 형태이며, 같은 문구를 연속으로 띄워도 다시 보이도록 id로 타이머를 재시작한다.
 */
function Toast({ toast, onClose }) {
  useEffect(() => {
    if (!toast) {
      return undefined;
    }
    const timer = setTimeout(onClose, AUTO_HIDE_MS);
    return () => clearTimeout(timer);
  }, [toast, onClose]);

  if (!toast) {
    return null;
  }

  return (
    <div className="app-toast" role="status" aria-live="polite">
      {toast.message}
    </div>
  );
}

export default Toast;
